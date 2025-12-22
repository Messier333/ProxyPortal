package com.messier333.proxyportal.portal.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPortalLink is a Querydsl query type for PortalLink
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPortalLink extends EntityPathBase<PortalLink> {

    private static final long serialVersionUID = -376001457L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPortalLink portalLink = new QPortalLink("portalLink");

    public final QPortalCategory category;

    public final StringPath icon = createString("icon");

    public final StringPath iconColor = createString("iconColor");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath name = createString("name");

    public final NumberPath<Integer> sortOrder = createNumber("sortOrder", Integer.class);

    public final StringPath url = createString("url");

    public QPortalLink(String variable) {
        this(PortalLink.class, forVariable(variable), INITS);
    }

    public QPortalLink(Path<? extends PortalLink> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPortalLink(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPortalLink(PathMetadata metadata, PathInits inits) {
        this(PortalLink.class, metadata, inits);
    }

    public QPortalLink(Class<? extends PortalLink> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.category = inits.isInitialized("category") ? new QPortalCategory(forProperty("category"), inits.get("category")) : null;
    }

}

