package hk.edu.hku.cs7506.smartcompanion.data.repository;

public interface RepositoryCallback<T> {
    void onSuccess(T data);

    void onError(String message);
}

