package com.example.kata.bank.service.delivery.application

import spark.Service
import spark.kotlin.Http

abstract class SparkAdapter : ApplicationEngine {
    protected var httpService: Http = Http(Service.ignite())

    override fun start(port: Int): SparkAdapter {
        val http = httpService
                .port(port)

        configurePaths(http)
        return this
    }

    override fun stop() {
        httpService.stop()
    }

    protected abstract fun configurePaths(http: Http)
}