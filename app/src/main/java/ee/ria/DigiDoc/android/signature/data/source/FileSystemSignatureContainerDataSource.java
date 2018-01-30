package ee.ria.DigiDoc.android.signature.data.source;

import com.google.common.collect.ImmutableList;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import javax.inject.Inject;

import ee.ria.DigiDoc.android.main.settings.SettingsDataStore;
import ee.ria.DigiDoc.android.signature.data.SignatureContainerDataSource;
import ee.ria.DigiDoc.android.utils.files.FileStream;
import ee.ria.DigiDoc.android.utils.files.FileSystem;
import ee.ria.mopplib.data.DataFile;
import ee.ria.mopplib.data.Signature;
import ee.ria.mopplib.data.SignedContainer;
import io.reactivex.Completable;
import io.reactivex.Single;

import static com.google.common.io.Files.getNameWithoutExtension;

public final class FileSystemSignatureContainerDataSource implements SignatureContainerDataSource {

    private final FileSystem fileSystem;
    private final SettingsDataStore settingsDataStore;

    @Inject FileSystemSignatureContainerDataSource(FileSystem fileSystem,
                                                   SettingsDataStore settingsDataStore) {
        this.fileSystem = fileSystem;
        this.settingsDataStore = settingsDataStore;
    }

    @Override
    public Single<File> addContainer(ImmutableList<FileStream> fileStreams, boolean forceCreate) {
        return Single.fromCallable(() -> {
            File containerFile;
            if (!forceCreate && fileStreams.size() == 1
                    && SignedContainer.isContainer(fileSystem.cache(fileStreams.get(0)))) {
                FileStream fileStream = fileStreams.get(0);
                containerFile = fileSystem.addSignatureContainer(fileStream);
            } else {
                String containerName = String.format(Locale.US, "%s.%s",
                        getNameWithoutExtension(fileStreams.get(0).displayName()),
                        settingsDataStore.getFileType());
                containerFile = fileSystem.generateSignatureContainerFile(containerName);
                SignedContainer.create(containerFile, cacheFileStreams(fileStreams));
            }
            return containerFile;
        });
    }

    @Override
    public Single<SignedContainer> get(File containerFile) {
        return Single.fromCallable(() -> SignedContainer.open(containerFile));
    }

    @Override
    public Completable addDocuments(File containerFile, ImmutableList<FileStream> documentStreams) {
        return Completable.fromAction(() ->
                SignedContainer
                        .open(containerFile)
                        .addDataFiles(cacheFileStreams(documentStreams)));
    }

    @Override
    public Completable removeDocument(File containerFile, DataFile document) {
        return Completable.fromAction(() ->
                SignedContainer
                        .open(containerFile)
                        .removeDataFile(document));
    }

    @Override
    public Single<File> getDocumentFile(File containerFile, DataFile document) {
        return Single.fromCallable(() ->
                SignedContainer
                        .open(containerFile)
                        .getDataFile(document, fileSystem.getCacheDir()));
    }

    @Override
    public Completable removeSignature(File containerFile, Signature signature) {
        return Completable.fromAction(() ->
                SignedContainer
                        .open(containerFile)
                        .removeSignature(signature));
    }

    @Override
    public Completable addSignature(File containerFile, String signature) {
        return Completable.fromAction(() ->
                SignedContainer
                        .open(containerFile)
                        .addAdEsSignature(signature.getBytes()));
    }

    private ImmutableList<File> cacheFileStreams(ImmutableList<FileStream> fileStreams) throws
            IOException {
        ImmutableList.Builder<File> fileBuilder = ImmutableList.builder();
        for (FileStream fileStream : fileStreams) {
            fileBuilder.add(fileSystem.cache(fileStream));
        }
        return fileBuilder.build();
    }
}