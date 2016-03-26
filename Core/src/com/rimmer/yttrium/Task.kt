package com.rimmer.yttrium

enum class TaskState {waiting, finished, error}

inline fun <T> task(f: Task<T>.() -> Unit): Task<T> {
    val t = Task<T>()
    t.f()
    return t
}

fun <T> finished(v: T): Task<T> {
    val task = Task<T>()
    task.finish(v)
    return task
}

fun <T> failed(reason: Throwable): Task<T> {
    val task = Task<T>()
    task.fail(reason)
    return task
}

/** Represents a task that will be performed asynchronously and will either provide a result or fail. */
class Task<T> {
    /**
     * Indicates that the task should finish with the provided value.
     * @return This task.
     */
    fun finish(v: T): Task<T> {
        if(state == TaskState.waiting) {
            state = TaskState.finished
            cachedResult = v
            handler?.invoke(v, null)
        } else {
            throw IllegalStateException("This task already has a result")
        }
        return this
    }

    /**
     * Indicates that the task should fail with the provided reason.
     * @return This task.
     */
    fun fail(reason: Throwable): Task<T> {
        if(state == TaskState.waiting) {
            state = TaskState.error
            cachedError = reason
            handler?.invoke(null, reason)
        } else {
            throw IllegalStateException("This task already has a result")
        }
        return this
    }

    /**
     * Call the provided handler when finished.
     * @return This task.
     */
    inline fun onFinish(crossinline f: (T) -> Unit): Task<T> {
        handler = {r, e -> r?.let(f) }
        return this
    }

    /**
     * Call the provided handler when failed.
     * @return This task.
     */
    inline fun onFail(crossinline f: (Throwable) -> Unit): Task<T> {
        handler = {r, e -> e?.let(f) }
        return this
    }

    /** Maps the task through the provided function, returning a new task. */
    inline fun <U> map(crossinline f: (T) -> U) = mapMaybe {f(it!!)}

    /** Maps the task through the provided function, returning a new task. */
    inline fun <U> mapMaybe(crossinline f: (T?) -> U): Task<U> {
        val task = Task<U>()
        handler = {r, e ->
            if(e == null) {
                try {
                    task.finish(f(r))
                } catch(e: Throwable) {
                    task.fail(e)
                }
            } else {
                task.fail(e)
            }
        }
        return task
    }

    /** Maps the task through the provided functions, returning a new task. */
    inline fun <U> map(crossinline succeed: (T) -> U, crossinline fail: (Throwable) -> U) = mapMaybe({succeed(it!!)}, fail)

    /** Maps the task through the provided functions, returning a new task. */
    inline fun <U> mapMaybe(crossinline succeed: (T?) -> U, crossinline fail: (Throwable) -> U): Task<U> {
        val task = Task<U>()
        handler = {r, e ->
            if(e == null) {
                try {
                    task.finish(succeed(r))
                } catch(e: Throwable) {
                    task.fail(e)
                }
            } else {
                try {
                    task.finish(fail(e))
                } catch(e: Throwable) {
                    task.fail(e)
                }
            }
        }
        return task
    }

    /** Maps the task failure through the provided function, returning a new task. */
    inline fun catch(crossinline f: (Throwable) -> T) = map({it}, {f(it)})

    /** Runs the provided task generator on finish, returning the new task. */
    inline fun <U> then(crossinline f: (T) -> Task<U>) = thenMaybe {f(it!!)}

    /** Runs the provided task generator on finish, returning the new task. */
    inline fun <U> thenMaybe(crossinline f: (T?) -> Task<U>): Task<U> {
        val task = Task<U>()
        handler = {r, e ->
            if(e == null) {
                try {
                    val next = f(r)
                    next.handler = {r, e ->
                        if(e == null) {
                            task.finish(r!!)
                        } else {
                            task.fail(e)
                        }
                    }
                } catch(e: Throwable) {
                    task.fail(e)
                }
            } else {
                task.fail(e)
            }
        }
        return task
    }

    /** Runs the provided task generator on finish or failure, returning the new task. */
    inline fun <U> then(crossinline succeed: (T) -> Task<U>, crossinline fail: (Throwable) -> Task<U>) = thenMaybe({succeed(it!!)}, fail)

    /** Runs the provided task generator on finish or failure, returning the new task. */
    inline fun <U> thenMaybe(crossinline succeed: (T?) -> Task<U>, crossinline fail: (Throwable) -> Task<U>): Task<U> {
        val task = Task<U>()
        handler = {r, e ->
            if(e == null) {
                try {
                    val next = succeed(r)
                    next.handler = {r, e ->
                        if(e == null) {
                            task.finish(r!!)
                        } else {
                            task.fail(e)
                        }
                    }
                } catch(e: Throwable) {
                    task.fail(e)
                }
            } else {
                try {
                    val next = fail(e)
                    next.handler = { r, e ->
                        if (e == null) {
                            task.finish(r!!)
                        } else {
                            task.fail(e)
                        }
                    }
                } catch(e: Throwable) {
                    task.fail(e)
                }
            }
        }
        return task
    }

    /** The last result passed through the task, if any. */
    val result: T? get() = cachedResult

    /** The last error passed through the task, if any. */
    val error: Throwable? get() = cachedError

    /** The current task result handler. */
    var handler: ((T?, Throwable?) -> Unit)? = null
        set(v) {
            field = v
            if(state != TaskState.waiting) {
                v?.invoke(result, error)
            }
        }

    private var cachedResult: T? = null
    private var cachedError: Throwable? = null
    private var state = TaskState.waiting
}
