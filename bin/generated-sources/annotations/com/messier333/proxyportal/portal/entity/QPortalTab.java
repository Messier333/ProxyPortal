package com.messier333.proxyportal.portal.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPortalTab is a Querydsl query type for PortalTab
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPortalTab extends EntityPathBase<PortalTab> {

    private static final long serialVersionUID = -1397594976L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPortalTab portalTab = new QPortalTab("portalTab");

    public final StringPath backgroundUrl = createString("backgroundUrl");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath name = createString("name");

    public final NumberPath<Integer> sortOrder = createNumber("sortOrder", Integer.class);

    public final com.messier333.proxyportal.user.entity.QUser user;

    public QPortalTab(String variable) {
        this(PortalTab.class, forVariable(variable), INITS);
    }

    public QPortalTab(Path<? extends PortalTab> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPortalTab(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPortalTab(PathMetadata metadata, PathInits inits) {
        this(PortalTab.class, metadata, inits);
    }

    public QPortalTab(Class<? extends PortalTab> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new com.messier333.proxyportal.user.entity.QUser(forProperty("user")) : null;
    }

}

