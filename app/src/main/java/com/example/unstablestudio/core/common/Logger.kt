package com.example.unstablestudio.core.common

import android.util.Log

/**
 * Logger abstraction for centralized logging.
 * Prevents sensitive data leakage and provides consistent log format.
 */
interface Logger {
    fun d(tag: String, message: String, throwable: Throwable? = null)
    fun i(tag: String, message: String, throwable: Throwable? = null)
    fun w(tag: String, message: String, throwable: Throwable? = null)
    fun e(tag: String, message: String, throwable: Throwable? = null)
}

/**
 * Default Logger implementation using Android's Log utility.
 */
class AndroidLogger : Logger {
    override fun d(tag: String, message: String, throwable: Throwable?) {
        Log.d(tag, message, throwable)
    }

    override fun i(tag: String, message: String, throwable: Throwable?) {
        Log.i(tag, message, throwable)
    }

    override fun w(tag: String, message: String, throwable: Throwable?) {
        Log.w(tag, message, throwable)
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        Log.e(tag, message, throwable)
    }
}

/**
 * Singleton Logger instance for easy access.
 * Can be replaced with DI in the future.
 */
object AppLogger {
    private var logger: Logger = AndroidLogger()

    fun setLogger(newLogger: Logger) {
        logger = newLogger
    }

    fun d(tag: String, message: String, throwable: Throwable? = null) {
        logger.d(tag, message, throwable)
    }

    fun i(tag: String, message: String, throwable: Throwable? = null) {
        logger.i(tag, message, throwable)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        logger.w(tag, message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        logger.e(tag, message, throwable)
    }
}
