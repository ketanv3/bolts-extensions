package bolts;

/**
 * A function to be called after a promise completes.
 *
 * If you wish to have the Promise from a ContinuationPromise that does not return a Promise be cancelled
 * then throw a {@link java.util.concurrent.CancellationException} from the Continuation.
 *
 * @see Promise
 */
public interface ContinuationPromise<TTaskResult, TContinuationResult> {
    TContinuationResult then(Promise<TTaskResult> promise) throws Exception;
}
