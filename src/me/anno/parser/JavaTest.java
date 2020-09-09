package me.anno.parser;

import kotlin.jvm.functions.Function1;

public class JavaTest {
    void main(){
        Function1<Object, Boolean> func = SimpleExpressionParser.INSTANCE.isValue();
    }
}
