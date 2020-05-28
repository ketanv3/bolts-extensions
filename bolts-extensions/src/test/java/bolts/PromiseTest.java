package bolts;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PromiseTest {

    @Test
    public void testPrimitives() {
        Promise<Integer> resolve = Promise.resolve(123);
        Promise<Integer> reject = Promise.reject(new ArithmeticException());
        Promise<Integer> cancelled = Promise.cancelled();

        assertTrue(resolve.isCompleted());
        assertTrue(resolve.isSuccessful());
        assertFalse(resolve.isCancelled());
        assertFalse(resolve.isFaulted());
        assertNull(resolve.getError());
        assertEquals(Integer.valueOf(123), resolve.getResult());

        assertTrue(reject.isCompleted());
        assertFalse(reject.isSuccessful());
        assertFalse(reject.isCancelled());
        assertTrue(reject.isFaulted());
        assertTrue(reject.getError() instanceof ArithmeticException);
        assertNull(reject.getResult());

        assertTrue(cancelled.isCompleted());
        assertFalse(cancelled.isSuccessful());
        assertTrue(cancelled.isCancelled());
        assertFalse(cancelled.isFaulted());
        assertNull(cancelled.getError());
        assertNull(cancelled.getResult());
    }

    @Test
    public void testSleep() throws InterruptedException {
        long randomDelay = (long) (100 + 900 * Math.random());
        long startTime = System.currentTimeMillis();

        Promise<?> sleepPromise = Promise.sleep(randomDelay);
        assertFalse(sleepPromise.isCompleted());

        Promise.await(sleepPromise);
        assertTrue(sleepPromise.isCompleted());

        long timeTaken = System.currentTimeMillis() - startTime;
        assertTrue(timeTaken >= randomDelay);
    }

    @Test
    public void testAsynchronousChaining() {
        Promise<Integer> p1 = Promise.resolve("123")
                .then(p -> Promise.resolve(p.getResult() + "456"))
                .then(p -> Promise.resolve(Integer.valueOf(p.getResult())))
                .then(p -> Promise.resolve(p.getResult() * 2));

        assertTrue(p1.isSuccessful());
        assertEquals(Integer.valueOf(123456 * 2), p1.getResult());


        Promise<Integer> p2 = Promise.resolve("1234567")
                .then(p -> Promise.resolve(p.getResult().length()))
                .then(p -> {
                    if (p.getResult() % 2 == 1) {
                        throw new IllegalArgumentException("odd length not allowed");
                    }

                    return Promise.resolve(p.getResult());
                });

        assertTrue(p2.isCompleted());
        assertFalse(p2.isSuccessful());
        assertTrue(p2.getError() instanceof IllegalArgumentException);


        Promise<?> p3 = p2
                .except(p -> Promise.resolve(0), IllegalArgumentException.class)
                .then(p -> {
                    throw new Exception("another exception!");
                })
                .always(p -> Promise.resolve("done"));

        assertTrue(p3.isSuccessful());
        assertEquals("done", p3.getResult());
    }

    @Test
    public void testSynchronousChaining() {
        Promise<Integer> p1 = Promise.resolve("123")
                .thenReturn(p -> p.getResult() + "456")
                .thenReturn(p -> Integer.valueOf(p.getResult()));

        assertTrue(p1.isSuccessful());
        assertEquals(Integer.valueOf(123456), p1.getResult());


        Promise<Integer> p2 = Promise.resolve("123")
                .thenReturn(p -> p.getResult() + "4567")
                .thenReturn(p -> p.getResult().length())
                .thenReturn(p -> {
                    if (p.getResult() % 2 == 1) {
                        throw new IllegalArgumentException("odd length not allowed");
                    }

                    return p.getResult();
                });

        assertTrue(p2.isCompleted());
        assertFalse(p2.isSuccessful());
        assertTrue(p2.getError() instanceof IllegalArgumentException);


        Promise<String> p3 = p2
                .except(p -> Promise.resolve(0), IllegalArgumentException.class)
                .thenReturn(p -> {
                    throw new Exception("another exception!");
                })
                .alwaysReturn(p -> "done");

        assertTrue(p3.isSuccessful());
        assertEquals("done", p3.getResult());
    }

    @Test
    public void testParallelAll() throws InterruptedException {
        long timeStart = System.currentTimeMillis();

        Promise<String> p1 = Promise.sleep(100).thenReturn(p -> "123");
        Promise<String> p2 = Promise.sleep(250).thenReturn(p -> "456");
        Promise<String> p3 = Promise.sleep(180).thenReturn(p -> "789");
        Promise<List<Promise<String>>> promiseAll = Promise.all(Arrays.asList(p1, p2, p3));

        assertFalse(p1.isCompleted());
        assertFalse(p2.isCompleted());
        assertFalse(p3.isCompleted());
        assertFalse(promiseAll.isCompleted());

        List<Promise<String>> results = Promise.await(promiseAll);
        assertTrue(promiseAll.isSuccessful());

        assertTrue(results.get(0).isSuccessful());
        assertTrue(results.get(1).isSuccessful());
        assertTrue(results.get(2).isSuccessful());
        assertEquals("123", results.get(0).getResult());
        assertEquals("456", results.get(1).getResult());
        assertEquals("789", results.get(2).getResult());

        // Time taken should be >= max sleep but definitely less than combined sleep.
        long timeTaken = System.currentTimeMillis() - timeStart;
        assertTrue(timeTaken >= 250);
        assertTrue(timeTaken < 100 + 250 + 180);
    }

    @Test
    public void testParallelAny() throws InterruptedException {
        Promise<String> p1 = Promise.sleep(100).thenReturn(p -> "123");
        Promise<String> p2 = Promise.sleep(250).thenReturn(p -> "456");
        Promise<String> p3 = Promise.sleep(180).thenReturn(p -> "789");
        Promise<Promise<String>> promiseAny = Promise.any(Arrays.asList(p1, p2, p3));

        assertFalse(p1.isCompleted());
        assertFalse(p2.isCompleted());
        assertFalse(p3.isCompleted());
        assertFalse(promiseAny.isCompleted());

        Promise<String> result = Promise.await(promiseAny);
        assertTrue(promiseAny.isSuccessful());
        assertEquals("123", result.getResult());

        assertTrue(p1.isCompleted());
        assertFalse(p2.isCompleted());
        assertFalse(p3.isCompleted());
    }

    @Test
    public void testParallelAllWithError() throws InterruptedException {
        Promise<String> p1 = Promise.sleep(100).thenReturn(p -> "123");
        Promise<String> p2 = Promise.sleep(250).thenReturn(p -> "456");
        Promise<String> p3 = Promise.sleep(180).then(p -> Promise.reject(new IllegalArgumentException()));
        Promise<List<Promise<String>>> promiseAll = Promise.all(Arrays.asList(p1, p2, p3));

        assertFalse(p1.isCompleted());
        assertFalse(p2.isCompleted());
        assertFalse(p3.isCompleted());
        assertFalse(promiseAll.isCompleted());

        Promise.await(promiseAll);

        assertTrue(p1.isSuccessful());
        assertTrue(p2.isSuccessful());
        assertFalse(p3.isSuccessful());
        assertFalse(promiseAll.isSuccessful());

        assertTrue(p3.getError() instanceof IllegalArgumentException);
        assertTrue(promiseAll.getError() instanceof IllegalArgumentException);
    }

    @Test
    public void testParallelAnyWithError() throws InterruptedException {
        Promise<String> p1 = Promise.sleep(100).thenReturn(p -> "123");
        Promise<String> p2 = Promise.sleep(250).thenReturn(p -> "456");
        Promise<String> p3 = Promise.sleep(180).then(p -> Promise.reject(new IllegalArgumentException()));
        Promise<Promise<String>> promiseAny = Promise.any(Arrays.asList(p1, p2, p3));

        assertFalse(p1.isCompleted());
        assertFalse(p2.isCompleted());
        assertFalse(p3.isCompleted());
        assertFalse(promiseAny.isCompleted());

        Promise.await(promiseAny);
        assertTrue(promiseAny.isSuccessful());
        assertEquals("123", promiseAny.getResult().getResult());

        assertTrue(p1.isSuccessful());
        assertFalse(p2.isCompleted());
        assertFalse(p3.isCompleted());
    }

    @Test
    public void testCreatePromise() {
        TaskCompletionSource<String> tcs1 = new TaskCompletionSource<>();
        tcs1.setResult("done");
        Promise<String> p1 = Promise.of(tcs1.getTask());
        assertTrue(p1.isSuccessful());

        TaskCompletionSource<String> tcs2 = new TaskCompletionSource<>();
        tcs2.setError(new IllegalArgumentException());
        Promise<String> p2 = Promise.of(tcs2.getTask());
        assertFalse(p2.isSuccessful());
        assertTrue(p2.isFaulted());
        assertTrue(p2.getError() instanceof IllegalArgumentException);

        TaskCompletionSource<String> tcs3 = new TaskCompletionSource<>();
        tcs3.setCancelled();
        tcs3.trySetResult("done but cancelled");
        Promise<String> p3 = Promise.of(tcs3.getTask());
        assertFalse(p3.isSuccessful());
        assertTrue(p3.isCancelled());
    }

}
