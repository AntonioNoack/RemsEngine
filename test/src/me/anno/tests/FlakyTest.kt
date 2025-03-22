package me.anno.tests

/**
 * tests annotated with this are flaky and known to only pass when running alone
 * */
annotation class FlakyTest(val msg: String = "")
