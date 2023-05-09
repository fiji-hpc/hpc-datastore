package cz.it4i.fiji.datastore.security;

import javax.interceptor.InvocationContext;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public class GroupsAuthentication extends Authentication{
    public void checkAuthorization(InvocationContext ctx) {
        Method method = ctx.getMethod();
        Parameter[] parameters = method.getParameters();
        Object uuidParam;
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            if (parameter.getName().equals("uuid")) {
                uuidParam= ctx.getParameters()[i];
                break;
            }
        }
    }
}
