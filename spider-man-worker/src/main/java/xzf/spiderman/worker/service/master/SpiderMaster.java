package xzf.spiderman.worker.service.master;

import xzf.spiderman.common.event.Event;
import xzf.spiderman.common.event.EventListener;
import xzf.spiderman.starter.curator.CuratorFacade;
import xzf.spiderman.worker.entity.SpiderCnf;
import xzf.spiderman.worker.service.*;
import xzf.spiderman.worker.service.event.SubmitSpiderEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 1. 选举出boss
 *
 * 2. 接受到调度任务，从数据中load到spider配置，获取爬虫集群信息
 *
 * 3. 创建任务 start, stop。 发布给每一台爬虫 http
 *
 * 4. 创建zk的工作节点， uuid。 并监控长的变化
 *
 * 5. 假如监听到zk工作目录的所有worker completed = true ，遍历发布stop消息给爬虫(http)
 *
 */
public class SpiderMaster implements EventListener
{
    private SpiderTaskRepository taskRepository;
    private CuratorFacade curatorFacade;
    private SpiderQueueProducer queueProducer;
    private SpiderNotifier spiderNotifier;

    public SpiderMaster(
            SpiderTaskRepository taskRepository,
            CuratorFacade curatorFacade,
            SpiderQueueProducer queueProducer,
            SpiderNotifier spiderNotifier)
    {
        this.taskRepository = taskRepository;
        this.curatorFacade = curatorFacade;
        this.queueProducer = queueProducer;
        this.spiderNotifier = spiderNotifier;
    }

    private List<SpiderTask> buildInitSpiderTaskRuntimeData(GroupSpiderKey key, List<SpiderCnf> cnfs)
    {
        List<SpiderTask> tasks = new ArrayList<>();
        for (SpiderCnf cnf : cnfs) {
            SpiderKey spiderKey = key.toSpiderKey(cnf.getId());
            SpiderTask data = SpiderTask.newInitTask(spiderKey);
            tasks.add(data);
        }
        return tasks;
    }

    public class SubmitSpiderHandler
    {
        private final GroupSpiderKey key;
        private final List<SpiderCnf> cnfs;

        public SubmitSpiderHandler(GroupSpiderKey key, List<SpiderCnf> cnfs) {
            this.key = key;
            this.cnfs = cnfs;
        }

        public void handle()
        {
            // 1. 保存Task数据信息到store中
            taskRepository.putAllAndLock(key, buildInitSpiderTaskRuntimeData(key, cnfs)); //init

            // 2.创建dispatcher
            SpiderDispatcher dispatcher = new SpiderDispatcher(key, cnfs);

            // 3. zk中创建目录，并监控
            SpiderWatcher.PreCloseCallback preCloseCallback = () -> {
                dispatcher.dispatchClose();
            };
            SpiderWatcher.CloseCallback closeCallback = ()->{
                taskRepository.removeAll(key);
                queueProducer.clear(key);
                spiderNotifier.notifyClosed(key);
            };

            SpiderWatcher watcher = SpiderWatcher.builder(curatorFacade)
                    .withStore(taskRepository)
                    .withKey(key)
                    .preCloseCallback(preCloseCallback)
                    .closeCallback(closeCallback)
                    .build();
            watcher.watchAutoClose();

            // 4. 发送任务到Spider Scheduler Queue中
            queueProducer.sendTasks(key, cnfs);

            // 5. 发送给slave，开始爬虫任务 ->  slave , zkCli -> create_path -> /worker/spider-task/{groupId}/{spiderId}/spider1-(data:ip,port, conf.  running)
            dispatcher.dispatchStart();
        }
    }

    //
    @Override
    public boolean supportEventType(Class<? extends Event> clazz) {
        return SubmitSpiderEvent.class.equals(clazz);
    }

    @Override
    public void onEvent(Event event)
    {
        if(event instanceof SubmitSpiderEvent)
        {
            onSubmitSpiderEvent( (SubmitSpiderEvent) event );
        }
    }

    private void onSubmitSpiderEvent(SubmitSpiderEvent event)
    {
        SubmitSpiderHandler handler = new SubmitSpiderHandler(event.getKey(), event.getAvailableCnfs());
        handler.handle();
    }
}
