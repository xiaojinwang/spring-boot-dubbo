package org.mvnsearch.spring.boot.dubbo;

import com.alibaba.dubbo.config.annotation.DubboService;
import com.alibaba.dubbo.config.spring.schema.DubboBeanDefinitionParser;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * dubbo endpoint
 *
 * @author linux_china
 */
public class DubboEndpoint extends AbstractEndpoint implements ApplicationContextAware {
    private DubboProperties dubboProperties;
    private ApplicationContext applicationContext;

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Autowired
    public void setDubboProperties(DubboProperties dubboProperties) {
        this.dubboProperties = dubboProperties;
    }

    public DubboEndpoint() {
        super("dubbo", false, true);
    }

    public Object invoke() {
        Map<String, Object> info = new HashMap<String, Object>();
        Boolean serverMode = false;
        String[] beanNames = applicationContext.getBeanNamesForAnnotation(EnableDubboConfiguration.class);
        if (beanNames != null && beanNames.length > 0) {
            serverMode = true;
        }
        if (serverMode) {
            info.put("server", true);
            info.put("port", dubboProperties.getPort());
        }
        info.put("app", dubboProperties.getApp());
        info.put("registry", dubboProperties.getRegistry());
        info.put("protocol", dubboProperties.getProtocol());
        //published services
        Map<String, List<String>> publishedInterfaceList = new HashMap<String, List<String>>();
        Map<String, Object> dubboBeans = applicationContext.getBeansWithAnnotation(DubboService.class);
        for (Map.Entry<String, Object> entry : dubboBeans.entrySet()) {
            Object bean = entry.getValue();
            Class<?> interfaceClass = bean.getClass().getAnnotation(DubboService.class).interfaceClass();
            String interfaceClassCanonicalName = interfaceClass.getCanonicalName();
            if (!interfaceClassCanonicalName.equals("void")) {
                List<String> methodNames = new ArrayList<String>();
                for (Method method : interfaceClass.getMethods()) {
                    methodNames.add(method.getName());
                }
                publishedInterfaceList.put(interfaceClassCanonicalName, methodNames);
            }
        }
        if (!publishedInterfaceList.isEmpty()) {
            info.put("publishedInterfaces", publishedInterfaceList);
        }
        //subscribed services
        Map<String, String> referenceBeanList = DubboBeanDefinitionParser.referenceBeanList;
        if (!referenceBeanList.isEmpty()) {
            try {
                Map<String, List<String>> subscribedInterfaceList = new HashMap<String, List<String>>();
                for (Map.Entry<String, String> entry : referenceBeanList.entrySet()) {
                    Class clazz = Class.forName(entry.getValue());
                    ArrayList<String> methodNames = new ArrayList<String>();
                    for (Method method : clazz.getMethods()) {
                        methodNames.add(method.getName());
                    }
                    subscribedInterfaceList.put(clazz.getCanonicalName(), methodNames);
                }
                info.put("subscribedInterfaces", subscribedInterfaceList);
            } catch (Exception ignore) {

            }
        }
        return info;
    }
}
