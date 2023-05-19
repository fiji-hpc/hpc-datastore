package cz.it4i.fiji.datastore.security;

import cz.it4i.fiji.datastore.register_service.Dataset;
import lombok.Getter;
import lombok.Setter;

import javax.interceptor.InvocationContext;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.EnumSet;
import java.util.List;

public class GroupsAuthentication extends Authentication{
    @Getter
    @Setter
    private List<Dataset> datasets;
    @Setter
    @Getter
    private EnumSet<PermissionType> permissionType;
    public void checkAuthorization(InvocationContext ctx) {
        Method method = ctx.getMethod();
        Parameter[] parameters = method.getParameters();
        Object uuidParam = null;
        boolean found=false;
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            if (parameter.getName().equals("uuid")) {
                uuidParam= ctx.getParameters()[i];
                found=true;
                break;
            }
        }
        if(found) {
            for(int i = 0; i < datasets.size(); i++) {
                if(datasets.get(i).getUuid().equals(uuidParam)) {
                    return;
                }
            }
            throw new UnauthorizedAccessException(getAccessToken());
        }
    }
}
