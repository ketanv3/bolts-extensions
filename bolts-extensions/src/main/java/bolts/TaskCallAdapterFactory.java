package bolts;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.CancellationException;

import javax.annotation.Nullable;

import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Callback;
import retrofit2.HttpException;
import retrofit2.Response;
import retrofit2.Retrofit;

public class TaskCallAdapterFactory extends CallAdapter.Factory {

    public static TaskCallAdapterFactory create() {
        return new TaskCallAdapterFactory();
    }

    @Nullable
    @Override
    public CallAdapter<?, ?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
        if (getRawType(returnType) != Task.class) {
            return null;
        }

        if (!(returnType instanceof ParameterizedType)) {
            throw new IllegalStateException(
                    "Task return type must be parameterized  as Task<Foo> or Task<? extends Foo>");
        }
        Type innerType = getParameterUpperBound(0, (ParameterizedType) returnType);

        if (getRawType(innerType) != Response.class) {
            // Generic type is not Response<T>. Use it for body-only adapter.
            return new BodyCallAdapter<>(innerType);
        }

        // Generic type is Response<T>. Extract T and create the Response version of the adapter.
        if (!(innerType instanceof ParameterizedType)) {
            throw new IllegalStateException(
                    "Response must be parameterized as Response<Foo> or Response<? extends Foo>");
        }
        Type responseType = getParameterUpperBound(0, (ParameterizedType) innerType);
        return new ResponseCallAdapter<>(responseType);
    }

    private static final class BodyCallAdapter<R> implements CallAdapter<R, Task<R>> {
        private final Type responseType;

        BodyCallAdapter(Type responseType) {
            this.responseType = responseType;
        }

        @Override
        public Type responseType() {
            return responseType;
        }

        @Override
        public Task<R> adapt(final Call<R> call) {
            final TaskCompletionSource<R> tcs = new TaskCompletionSource<>();

            call.enqueue(new Callback<R>() {
                @Override
                public void onResponse(Call<R> call, Response<R> response) {
                    setResponseResult(response, tcs);
                }

                @Override
                public void onFailure(Call<R> call, Throwable t) {
                    tcs.setError(new Exception(t));
                }
            });

            return tcs.getTask();
        }

        private void setResponseResult(Response<R> response, TaskCompletionSource<R> tcs) {
            try {
                if (response.isSuccessful()) {
                    tcs.setResult(response.body());
                } else {
                    tcs.setError(new HttpException(response));
                }
            } catch (CancellationException e) {
                tcs.setCancelled();
            } catch (Exception e) {
                tcs.setError(e);
            }
        }
    }

    private static final class ResponseCallAdapter<R> implements CallAdapter<R, Task<Response<R>>> {
        private final Type responseType;

        ResponseCallAdapter(Type responseType) {
            this.responseType = responseType;
        }

        @Override
        public Type responseType() {
            return responseType;
        }

        @Override
        public Task<Response<R>> adapt(final Call<R> call) {
            final TaskCompletionSource<Response<R>> tcs = new TaskCompletionSource<>();

            call.enqueue(new Callback<R>() {
                @Override
                public void onResponse(Call<R> call, Response<R> response) {
                    setResultResponse(response, tcs);
                }

                @Override
                public void onFailure(Call<R> call, Throwable t) {
                    tcs.setError(new Exception(t));
                }
            });

            return tcs.getTask();
        }

        private void setResultResponse(Response<R> response, TaskCompletionSource<Response<R>> tcs) {
            try {
                tcs.setResult(response);
            } catch (CancellationException e) {
                tcs.setCancelled();
            } catch (Exception e) {
                tcs.setError(e);
            }
        }
    }

}
