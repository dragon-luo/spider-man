package xzf.spiderman.common.configuration.web;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(SpiderManCookieConfiguration.class)
public @interface EnableSpiderManCookie
{
}
