package ee.ria.DigiDoc.android.utils.mvi;

import android.util.Log;

import androidx.lifecycle.ViewModel;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.core.ObservableTransformer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.observables.ConnectableObservable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;
import timber.log.Timber;

public abstract class BaseMviViewModel<
        I extends MviIntent,
        S extends MviViewState,
        A extends MviAction,
        R extends MviResult<S>> extends ViewModel implements MviViewModel<I, S> {

    private final Subject<I> intentSubject;
    private final ConnectableObservable<S> viewStateObservable;
    private final Disposable disposable;

    protected BaseMviViewModel(ObservableTransformer<A, R> processor) {
        logD("Constructor");
        intentSubject = PublishSubject.create();
        viewStateObservable = intentSubject
                .compose(this::initialIntentFilter)
                .doOnNext(mviLog("Intent"))
                .map(this::action)
                .doOnNext(mviLog("Action"))
                .compose(processor)
                .doOnNext(mviLog("Result"))
                .scan(initialViewState(), (viewState, result) -> result.reduce(viewState))
                .doOnNext(mviLog("ViewState"))
                .replay(1);
        disposable = viewStateObservable.connect();
    }

    @Override
    public void process(Observable<I> intents) {
        intents.subscribe(intentSubject);
    }

    @Override
    public Observable<S> viewStates() {
        return viewStateObservable;
    }

    protected abstract Class<? extends I> initialIntentType();

    protected abstract A action(I intent);

    protected abstract S initialViewState();

    @Override
    protected void onCleared() {
        super.onCleared();
        logD("onCleared");
        disposable.dispose();
    }

    private ObservableSource<I> initialIntentFilter(Observable<I> intents) {
        Class<? extends I> initialIntentType = initialIntentType();
        return intents.publish(shared -> Observable.merge(
                shared.ofType(initialIntentType).take(1),
                shared.filter(intent -> !initialIntentType.isInstance(intent))));
    }

    private <T> Consumer<T> mviLog(String message) {
        return item -> logD("%s: %s", message, item);
    }

    private void logD(String message, Object... args) {
        Timber.tag(getClass().getSimpleName());
        Timber.log(Log.DEBUG, message, args);
    }
}
