package com.messier333.proxyportal.portal.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPortalCategory is a Querydsl query type for PortalCategory
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPortalCategory extends EntityPathBase<PortalCategory> {

    private static final long serialVersionUID = 1723238099L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPortalCategory portalCategory = new QPortalCategory("portalCategory");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath name = createString("name");

    public final NumberPath<Integer> sortOrder = createNumber("sortOrder", Integer.class);

    public final QPortalTab tab;

    public QPortalCategory(String variable) {
        this(PortalCategory.class, forVariable(variable), INITS);
    }

    public QPortalCategory(Path<? extends PortalCategory> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPortalCategory(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPortalCategory(PathMetadata metadata, PathInits inits) {
        this(PortalCategory.class, metadata, inits);
    }

    public QPortalCategory(Class<? extends PortalCategory> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.tab = inits.isInitialized("tab") ? new QPortalTab(forProperty("tab"), inits.get("tab")) : null;
    }

}

