package com.zeusgarage.mobile

fun main() {
    check(moneyInputToCents("12.34") == 1234L)
    check(moneyInputToCents("-5") == null)
    check(moneyInputToCents("3.999") == 400L)
    check(ServiceTask(1,1,"Oil",5000,30000,"").dueMiles == 35000)
    check(ServiceTask(1,1,"Oil",5000,30000,"").remaining(36500) == -1500)
    check(ServiceTask(1,1,"Oil",0,null,"").remaining(null) == null)
    val fillups = listOf(
        FuelEntry(1, 1, "2026-01-01", 10000, 25.0, 8100, true),
        FuelEntry(2, 1, "2026-01-07", 10150, 10.0, 3200, false),
        FuelEntry(3, 1, "2026-01-14", 10350, 15.0, 4900, true)
    )
    val mpg = latestFullToFullMpg(fillups)
    check(mpg != null && kotlin.math.abs(mpg - 14.0) < 0.00001) { "MPG: $mpg" }
    check(latestFullToFullMpg(fillups.take(1)) == null)
    val car = Vehicle(1,"ZEUS","2019","Ford","F-250 Super Duty","6.7L Power Stroke",null,"")
    check(car.odometer == null)
    println("ZEUS domain smoke tests PASSED")
}
