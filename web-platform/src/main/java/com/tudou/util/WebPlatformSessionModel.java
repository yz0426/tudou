package com.tudou.util;

import com.google.common.collect.Maps;
import com.tudou.common.json.JsonUtil;
import com.tudou.common.util.A;
import com.tudou.common.util.U;
import com.tudou.base.model.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Setter
@Getter
@NoArgsConstructor
@Accessors(chain = true)
@ToString
class WebPlatformSessionModel implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 默认未登录用户的 id */
    private static final Long DEFAULT_ID = 0L;
    /** 默认未登录用户的 name */
    private static final String DEFAULT_NAME = "未登录用户";
    /** 超级管理员账号 */
    private static final List<String> SUPER_USER = Arrays.asList("erpadmin");


    // ========== 存放在 session 中的数据 ==========

    /** 存放在 session 中的(用户 id) */
    private Long id;
    /** 存放在 session 中的(用户名) */
    private String name;
    /** 存放在 session 中的(所属系统) */
    private Integer systemId;
    /** 存放在 session 中的(父账户ID) */
    private Long parentId;

    /**
     * <pre>
     * 存放在 session 中的(指定域名的权限列表). 验证权限时会基于域名
     * {
     *   "域名1": [{"url": "/user", "method": "post"}, {"url": "/product", "method": "post"}],
     *   "域名2": [{"url": "/stock", "method": "post"}, {"url": "/coupon", "method": "post"}]
     * }
     * </pre>
     */
    private Map<String, List<Permission>> domainInfoMap = Maps.newHashMap();

    // ========== 存放在 session 中的数据 ==========


    /** 当前用户在指定域名下是否已登录. 已登录就返回 true */
    boolean wasLogin(String domain) {
        return !Objects.equals(DEFAULT_ID, id) && !Objects.equals(DEFAULT_NAME, name)
                && U.isNotBlank(domain) && A.isNotEmpty(domainInfoMap) && A.isNotEmpty(domainInfoMap.get(domain));
    }
    /** 是否是超级管理员. 是就返回 true */
    boolean wasSuper() {
        return SUPER_USER.contains(name);
    }
    /** 在指定域名下, 是否有对指定 url 的访问权限. 有就返回 true */
    boolean hasPermission(String domain, String url, String method) {
        if (A.isNotEmpty(domainInfoMap)) {
            List<Permission> permissionList = domainInfoMap.get(domain);
            if (A.isNotEmpty(permissionList)) {
                for (Permission permission : permissionList) {
                    String permissionUrl = permission.getUrl();
//                    String permissionMethod = permission.getMethod().toUpperCase();

                    // 全字 或 通配. 通配是指: 如果配置的 url 是 /user/*, 传进来的是 /user/info 也可以通过
                    if ((url.equals(permissionUrl) || url.matches(permissionUrl.replace("*", ".*")))
                        // 全字 或 通配. 通配是指: 如果配置的 method 是 * 即可
                        //&& (permissionMethod.contains(method.toUpperCase()) || "*".equals(permissionMethod))
                            ) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** 将 域名、账号及权限 组装成放进 session 的数据对象 */
    static WebPlatformSessionModel assemblyData(WebPlatformSessionModel sessionModel, String domain,
                                                User account, List<com.tudou.base.model.Permission> permissions) {
        if (U.isBlank(domain) || U.isBlank(account) || A.isEmpty(permissions)) {
            return sessionModel;
        }
        WebPlatformSessionModel session = JsonUtil.convert(account, WebPlatformSessionModel.class);
        List<Permission> permissionList = JsonUtil.convertList(permissions, Permission.class);
        // 是否是第一次向 session 中存放
        if (U.isBlank(sessionModel)) {
            // session 中没有信息就放进去
            session.setDomainInfoMap(A.maps(domain, permissionList));
        } else {
            // session 中有信息就把当前域名对应的权限加进去
            session.getDomainInfoMap().put(domain, permissionList);
        }
        return session;
    }

    /** 未登录时的默认用户信息 */
    static WebPlatformSessionModel defaultUser() {
        return new WebPlatformSessionModel().setId(DEFAULT_ID).setName(DEFAULT_NAME);
    }

    @Setter
    @Getter
    @ToString
    private static class Permission implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 权限路径 */
        private String url;
        /** 权限方法(get,head,post,put,delete), 多个用逗号隔开, 通配 * 表示匹配所有 */
        private String method;
    }
}
