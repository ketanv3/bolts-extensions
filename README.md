# bolts-extensions <img src="./bolts.png" width="80" align="right" />

Bolts is a collection of low-level libraries designed to make developing mobile apps easier.

Bolts Extensions adds additional capabilities to [Bolts](https://github.com/BoltsFramework/Bolts-Android) to simplify development even further. This includes:

* Promise API
* Retrofit Call Adapter (Task + Promise)


## Installation

**Step 1:** Add JitPack repository to your project level build.gradle file.

```groovy
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

**Step 2:** Add the bolts-extensions dependency to your module's build.gradle file.

```groovy
dependencies {
    implementation 'com.github.ketanv3:bolts-extensions:<latest-release>'
}
```

Latest release tags can be found [here](https://github.com/ketanv3/bolts-extensions/releases).


## Usage Instructions

### Promise API
The idea behind `Promise` is to avoid nested "pyramid" code typically found when we are dealing with a lot of asynchronous code involving callbacks.

Bolts already provides us with `Task` that are fully composable, allows chaining and parallel execution with complex error handling; without dealing with spaghetti code of callbacks.

`Promise` builds on top of `Task` to provide additional capabilities with a much simpler usage API. It is also made analogous to the [Promise](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise) API found in JavaScript!

#### Primitives
These primitives provide some ready-made promises.
* `Promise.resolve(TResult value)` - returns a resolved promise with the given value as the result.
* `Promise.reject(Exception error)` - returns a rejected promise with the given error.
* `Promise.cancelled()` - returns a cancelled promise.
* `Promise.sleep(long millis)` - returns a promise that resolves after the given sleep amount.

#### Promises in Series
These methods allow chaining of promises.
* `.then(Promise<ContinuationResult> nextPromise)` - chains another promise on successful execution of current promise.
* `.except(Promise<TResult> nextPromise, Class exceptionClass)` - handles the given exception if thrown from any promise in the above chain.
* `.always(Promise<ContinuationResult> nextPromise)` - chains another promise regardless of successful/unsuccessful execution of the current promise.
* `.thenReturn(ContinuationResult nextResult)` - similar to then(), but returns the result directly.
* `.alwaysReturn(ContinuationResult nextResult)` - similar to always(), but returns the result directly.

#### Promises in Parallel
These methods allow parallel execution of multiple promises. These return another Promise.
* `Promise.all(List<Promise<TResult>> promises)` - resolves when all promises have resolved, or rejects if at least one promise has failed.
* `Promise.any(List<Promise<TResult>> promises)` - resolves when the first promise resolves.

#### Utilities
* `Promise.of(Task<TResult> task)` - creates a promise from a bolts.Task.
* `Promise.await(Promise<TResult> promise)` - awaits for the promise to complete and returns the result.

All existing features available in Bolts' Task are still available in Promise API as well. Check out more [here](https://github.com/BoltsFramework/Bolts-Android).

#### Examples

```java
// Promises in series:
Promise<String> p1 = Promise.sleep(500)
        .then(p -> Promise.sleep(400))
        .thenReturn(p -> "123") // Synchronous return
        .thenReturn(p -> p.getResult() + "456") // Synchronous return
        .then(p -> {
            if (p.getResult().length() % 2 == 1) {
                throw new IllegalArgumentException("odd length");
            }

            return Promise.resolve(p.getResult().length());
        })
        .except(p -> Promise.resolve(0), IllegalArgumentException.class)
        .always(p -> Promise.resolve("The length is: " + p.getResult()));

// Some more chained promises:
Promise<String> p2 = Promise.sleep(400).thenReturn(p -> "p2 done");
Promise<String> p3 = Promise.sleep(1000).then(p -> Promise.resolve("p3 done"));

// Promises in parallel:
Promise<List<Promise<String>>> promiseAll = Promise.all(Arrays.asList(p1, p2, p3));
Promise<Promise<String>> promiseAny = Promise.any(Arrays.asList(p1, p2, p3));

// Awaiting for a result (or failure). Note this would block the current thread.
List<Promise<String>> allResult = Promise.await(promiseAll);
for (Promise<String> p : allResult) {
    System.out.println(p.getResult());
}

promiseAny.alwaysReturn(p -> {
    System.out.println("First result is: " + p.getResult().getResult());
    return null;
});

// Delivering results to UI thread (or some other thread):
Promise.sleep(10000) // some long running operation
        .thenReturn(p -> "John Doe")
        .thenReturn(p -> {
            Toast.makeText(this, "Hello, " + p.getResult(), Toast.LENGTH_SHORT).show();
            return null;
        }, Promise.UI_THREAD_EXECUTOR);
```


### Retrofit Call Adapters
A Retrofit 2 `CallAdapter.Factory` for adapting Promises and Tasks. Bolts Extensions provides the following two factories:
* PromiseCallAdapterFactory
* TaskCallAdapterFactory

Add the required call adapter factories when building the Retrofit instance:

```java
Retrofit retrofit = new Retrofit.Builder()
    .baseUrl("https://example.com/")
    .addCallAdapterFactory(PromiseCallAdapterFactory.create())
    .addCallAdapterFactory(TaskCallAdapterFactory.create())
    .build();
```

Your service methods can now use any of the above types as their return type.

```java
interface MyService {
    @GET("/user")
    Promise<User> getUserPromise();

    @GET("/user")
    Task<User> getUserTask();
}
```


## Issues and Suggestions
If you encounter any issues or have suggestions, please [file an issue](https://github.com/ketanv3/bolts-extensions/issues) along with a detailed description. Remember to apply labels for easier tracking.


## Versioning
We use [SemVer](http://semver.org/) for versioning. For the available versions, see the [tags on this repository](https://github.com/ketanv3/bolts-extensions/tags)


## Authors
See the list of [contributors](https://github.com/ketanv3/bolts-extensions/contributors) who participated in this project.