/**
 * Custom implementation of Google's FileDataStoreFactory
 * 
 * Disables the faulty logging in Windows by removing
 * the setPermissionsToOwnerOnly static method. The user
 * should protect their credentials through their operating
 * system, as the implementation by Google will fail inside
 * the PhantomBot environment.
 * 
 */

package tv.phantombot.songlist;

import com.google.api.client.util.IOUtils;
import com.google.api.client.util.Maps;
import com.google.api.client.util.store.AbstractDataStoreFactory;
import com.google.api.client.util.store.AbstractMemoryDataStore;
import com.google.api.client.util.store.DataStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;

public class CustomFileDataStoreFactory extends AbstractDataStoreFactory {

  /** Directory to store data. */
  private final File dataDirectory;

  /**
   * @param dataDirectory data directory
   */
  public CustomFileDataStoreFactory(File dataDirectory) throws IOException {
    dataDirectory = dataDirectory.getCanonicalFile();
    this.dataDirectory = dataDirectory;
    // error if it is a symbolic link
    if (IOUtils.isSymbolicLink(dataDirectory)) {
      throw new IOException("unable to use a symbolic link: " + dataDirectory);
    }
    // create parent directory (if necessary)
    if (!dataDirectory.exists() && !dataDirectory.mkdirs()) {
      throw new IOException("unable to create directory: " + dataDirectory);
    }
  }

  /** Returns the data directory. */
  public final File getDataDirectory() {
    return dataDirectory;
  }

  @Override
  protected <V extends Serializable> DataStore<V> createDataStore(String id) throws IOException {
    return new FileDataStore<V>(this, dataDirectory, id);
  }

  /**
   * File data store that inherits from the abstract memory data store because the key-value pairs
   * are stored in a memory cache, and saved in the file (see {@link #save()} when changing values.
   *
   * @param <V> serializable type of the mapped value
   */
  static class FileDataStore<V extends Serializable> extends AbstractMemoryDataStore<V> {

    /** File to store data. */
    private final File dataFile;

    FileDataStore(CustomFileDataStoreFactory dataStore, File dataDirectory, String id)
        throws IOException {
      super(dataStore, id);
      this.dataFile = new File(dataDirectory, id);
      // error if it is a symbolic link
      if (IOUtils.isSymbolicLink(dataFile)) {
        throw new IOException("unable to use a symbolic link: " + dataFile);
      }
      // create new file (if necessary)
      if (dataFile.createNewFile()) {
        keyValueMap = Maps.newHashMap();
        // save the credentials to create a new file
        save();
      } else {
        // load credentials from existing file
        keyValueMap = IOUtils.deserialize(new FileInputStream(dataFile));
      }
    }

    @Override
    public void save() throws IOException {
      IOUtils.serialize(keyValueMap, new FileOutputStream(dataFile));
    }

    @Override
    public CustomFileDataStoreFactory getDataStoreFactory() {
      return (CustomFileDataStoreFactory) super.getDataStoreFactory();
    }
  }

}
