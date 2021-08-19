package com.github.tlkh.tsp.utils;


@FunctionalInterface
public interface FunctionWithTwoArgs<VAL1, VAL2, RET> {

    RET apply(VAL1 val1, VAL2 val2);

}