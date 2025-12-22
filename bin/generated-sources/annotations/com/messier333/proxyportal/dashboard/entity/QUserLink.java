package com.messier333.proxyportal.dashboard.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QUserLink is a Querydsl query type for UserLink
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserLink extends EntityPathBase<UserLink> {

    private static final long serialVersionUID = -876275434L;

    public static final QUserLink userLink = new QUserLink("userLink");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath userId = createString("userId");

    public QUserLink(String variable) {
        super(UserLink.class, forVariable(variable));
    }

    public QUserLink(Path<? extends UserLink> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUserLink(PathMetadata metadata) {
        super(UserLink.class, metadata);
    }

}

