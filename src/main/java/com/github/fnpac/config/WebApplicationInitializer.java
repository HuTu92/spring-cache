package com.github.fnpac.config;

import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

/**
 * Created by liuchunlong on 2018/2/12.
 */
public class WebApplicationInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {
    protected Class<?>[] getRootConfigClasses() {
        return new Class[0];
    }

    protected Class<?>[] getServletConfigClasses() {
        return new Class<?>[]{WebApplicationConfig.class};
    }

    protected String[] getServletMappings() {
        return new String[]{"/"};
    }
}
