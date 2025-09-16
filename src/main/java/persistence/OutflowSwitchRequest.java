package persistence;

import util.BaseBean;

import java.sql.Connection;

public interface OutflowSwitchRequest {

    boolean createModuleRequest(BaseBean request, Connection connection);
    boolean updateModuleRequest(BaseBean request, BaseBean unapprovedBean, Connection connection);
    boolean deactivateModuleRequest(BaseBean request, BaseBean unapprovedBean, Connection connection);
    boolean approveModuleRequest(BaseBean request, Connection connection);

}
