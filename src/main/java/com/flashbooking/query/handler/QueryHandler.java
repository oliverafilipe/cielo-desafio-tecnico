package com.flashbooking.query.handler;

import com.flashbooking.query.model.Query;

public interface QueryHandler<Q extends Query, R> {
  R handle(Q query);
}
