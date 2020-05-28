package bolts;

import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Promise<TResult> {

    // Pre-defined executors:
    private static final Executor IMMEDIATE_EXECUTOR = BoltsExecutors.immediate();
    public static final Executor BACKGROUND_EXECUTOR = BoltsExecutors.background();
    public static final Executor UI_THREAD_EXECUTOR = AndroidExecutors.uiThread();

    @Getter
    private Task<TResult> task;

    public static <TResult> Promise<TResult> of(Task<TResult> task) {
        return new Promise<>(task);
    }

    public static <TResult> Promise<TResult> resolve() {
        return Promise.of(Task.forResult(null));
    }

    public static <TResult> Promise<TResult> resolve(TResult value) {
        return Promise.of(Task.forResult(value));
    }

    public static <TResult> Promise<TResult> reject(Exception error) {
        return Promise.of(Task.forError(error));
    }

    public static <TResult> Promise<TResult> cancelled() {
        return Promise.of(Task.cancelled());
    }

    public static <TResult> Promise<TResult> sleep(long millis) {
        TaskCompletionSource<TResult> tcs = new TaskCompletionSource<>();

        new Thread(() -> {
            try {
                Thread.sleep(millis);
                tcs.setResult(null);
            } catch (InterruptedException e) {
                tcs.setError(e);
            }
        }).start();

        return Promise.of(tcs.getTask());
    }

    public static <TResult> Promise<List<Promise<TResult>>> all(List<Promise<TResult>> promises) {
        Collection<Task<TResult>> tasks = Collections2
                .transform(promises, promise -> Objects.requireNonNull(promise).getTask());

        return Promise
                .of(Task.whenAllResult(tasks))
                .thenReturn(promise -> promises);
    }

    public static <TResult> Promise<Promise<TResult>> any(List<Promise<TResult>> promises) {
        Collection<Task<TResult>> tasks = Collections2
                .transform(promises, p -> Objects.requireNonNull(p).getTask());

        return Promise
                .of(Task.whenAnyResult(tasks))
                .thenReturn(promise -> Iterables.find(promises, p -> Objects.requireNonNull(p).getTask() == promise.getResult()));
    }

    public static <TResult> TResult await(Promise<TResult> promise) throws InterruptedException {
        promise.getTask().waitForCompletion();
        return promise.getTask().getResult();
    }

    public <TContinuationResult> Promise<TContinuationResult> then(
            final ContinuationPromise<TResult, Promise<TContinuationResult>> continuation,
            final Executor executor,
            final CancellationToken ct) {
        Task<TContinuationResult> wrappedTask = getTask()
                .onSuccessTask(task -> continuation.then(this).getTask(), executor, ct);
        return Promise.of(wrappedTask);
    }

    public <TContinuationResult> Promise<TContinuationResult> then(
            final ContinuationPromise<TResult, Promise<TContinuationResult>> continuation) {
        return then(continuation, IMMEDIATE_EXECUTOR, null);
    }

    public <TContinuationResult> Promise<TContinuationResult> then(
            final ContinuationPromise<TResult, Promise<TContinuationResult>> continuation,
            final Executor executor) {
        return then(continuation, executor, null);
    }

    public <TContinuationResult> Promise<TContinuationResult> then(
            final ContinuationPromise<TResult, Promise<TContinuationResult>> continuation,
            final CancellationToken ct) {
        return then(continuation, IMMEDIATE_EXECUTOR, ct);
    }

    public <TContinuationResult> Promise<TContinuationResult> thenReturn(
            final ContinuationPromise<TResult, TContinuationResult> continuation,
            final Executor executor,
            final CancellationToken ct) {
        Task<TContinuationResult> wrappedTask = getTask()
                .onSuccess(task -> continuation.then(this), executor, ct);
        return Promise.of(wrappedTask);
    }

    public <TContinuationResult> Promise<TContinuationResult> thenReturn(
            final ContinuationPromise<TResult, TContinuationResult> continuation) {
        return thenReturn(continuation, IMMEDIATE_EXECUTOR, null);
    }

    public <TContinuationResult> Promise<TContinuationResult> thenReturn(
            final ContinuationPromise<TResult, TContinuationResult> continuation,
            final Executor executor) {
        return thenReturn(continuation, executor, null);
    }

    public <TContinuationResult> Promise<TContinuationResult> thenReturn(
            final ContinuationPromise<TResult, TContinuationResult> continuation,
            final CancellationToken ct) {
        return thenReturn(continuation, IMMEDIATE_EXECUTOR, ct);
    }

    public <TContinuationResult> Promise<TContinuationResult> always(
            final ContinuationPromise<TResult, Promise<TContinuationResult>> continuation,
            final Executor executor,
            final CancellationToken ct) {
        Task<TContinuationResult> wrappedTask = getTask()
                .continueWithTask(task -> continuation.then(this).getTask(), executor, ct);
        return Promise.of(wrappedTask);
    }

    public <TContinuationResult> Promise<TContinuationResult> always(
            final ContinuationPromise<TResult, Promise<TContinuationResult>> continuation) {
        return always(continuation, IMMEDIATE_EXECUTOR, null);
    }

    public <TContinuationResult> Promise<TContinuationResult> always(
            final ContinuationPromise<TResult, Promise<TContinuationResult>> continuation,
            final Executor executor) {
        return always(continuation, executor, null);
    }

    public <TContinuationResult> Promise<TContinuationResult> always(
            final ContinuationPromise<TResult, Promise<TContinuationResult>> continuation,
            final CancellationToken ct) {
        return always(continuation, IMMEDIATE_EXECUTOR, ct);
    }

    public <TContinuationResult> Promise<TContinuationResult> alwaysReturn(
            final ContinuationPromise<TResult, TContinuationResult> continuation,
            final Executor executor,
            final CancellationToken ct) {
        Task<TContinuationResult> wrappedTask = getTask()
                .continueWith(task -> continuation.then(this), executor, ct);
        return Promise.of(wrappedTask);
    }

    public <TContinuationResult> Promise<TContinuationResult> alwaysReturn(
            final ContinuationPromise<TResult, TContinuationResult> continuation) {
        return alwaysReturn(continuation, IMMEDIATE_EXECUTOR, null);
    }

    public <TContinuationResult> Promise<TContinuationResult> alwaysReturn(
            final ContinuationPromise<TResult, TContinuationResult> continuation,
            final Executor executor) {
        return alwaysReturn(continuation, executor, null);
    }

    public <TContinuationResult> Promise<TContinuationResult> alwaysReturn(
            final ContinuationPromise<TResult, TContinuationResult> continuation,
            final CancellationToken ct) {
        return alwaysReturn(continuation, IMMEDIATE_EXECUTOR, ct);
    }

    public Promise<TResult> except(
            final ContinuationPromise<TResult, Promise<TResult>> continuation,
            final Class exceptionClass,
            final Executor executor,
            final CancellationToken ct) {

        Task<TResult> wrappedTask = getTask().continueWithTask(task -> {
            if (task.isFaulted() && exceptionClass.isInstance(task.getError())) {
                return continuation.then(this).getTask();
            }

            return task;
        }, executor, ct);

        return Promise.of(wrappedTask);
    }

    public Promise<TResult> except(
            final ContinuationPromise<TResult, Promise<TResult>> continuation,
            final Class exceptionClass) {
        return except(continuation, exceptionClass, IMMEDIATE_EXECUTOR, null);
    }

    public Promise<TResult> except(
            final ContinuationPromise<TResult, Promise<TResult>> continuation,
            final Class exceptionClass,
            final Executor executor) {
        return except(continuation, exceptionClass, executor, null);
    }

    public Promise<TResult> except(
            final ContinuationPromise<TResult, Promise<TResult>> continuation,
            final Class exceptionClass,
            final CancellationToken ct) {
        return except(continuation, exceptionClass, IMMEDIATE_EXECUTOR, ct);
    }

    public boolean isCompleted() {
        return getTask().isCompleted();
    }

    public boolean isCancelled() {
        return getTask().isCancelled();
    }

    public boolean isFaulted() {
        return getTask().isFaulted();
    }

    public boolean isSuccessful() {
        return isCompleted() && !isCancelled() && !isFaulted();
    }

    public TResult getResult() {
        return getTask().getResult();
    }

    public Exception getError() {
        return getTask().getError();
    }

}
