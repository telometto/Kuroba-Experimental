/*
 * KurobaEx - *chan browser https://github.com/K1rakishou/Kuroba-Experimental/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.adamantcheese.chan.core.database;

import android.util.Log;

import androidx.annotation.Nullable;

import com.github.adamantcheese.chan.Chan;
import com.github.adamantcheese.chan.core.model.orm.Board;
import com.github.adamantcheese.chan.core.model.orm.Loadable;
import com.github.adamantcheese.chan.core.repository.BoardRepository;
import com.github.adamantcheese.chan.core.repository.SiteRepository;
import com.github.adamantcheese.chan.core.site.Site;
import com.github.adamantcheese.chan.utils.Logger;
import com.github.adamantcheese.model.data.descriptor.ChanDescriptor;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

public class DatabaseLoadableManager {
    private static final String TAG = "DatabaseLoadableManager";

    private DatabaseHelper helper;
    private DatabaseManager databaseManager;
    private SiteRepository siteRepository;
    private BoardRepository boardRepository;

    private Map<Loadable, Loadable> cachedLoadables = new HashMap<>();
    private Map<ChanDescriptor, Loadable> loadablesByDescriptors = new HashMap<>();

    public DatabaseLoadableManager(
            DatabaseHelper helper,
            DatabaseManager databaseManager
    ) {
        this.helper = helper;
        this.databaseManager = databaseManager;
    }

    private synchronized SiteRepository getSiteRepository() {
        if (siteRepository != null) {
            return siteRepository;
        }

        // TODO(dependency-cycles): get rid of dependency cycle
        // We have to use Chan.instance() here because we can't inject it normally since it will
        // create a dependency cycle
        siteRepository = Chan.instance(SiteRepository.class);
        return siteRepository;
    }

    private synchronized BoardRepository getBoardRepository() {
        if (boardRepository != null) {
            return boardRepository;
        }

        // TODO(dependency-cycles): get rid of dependency cycle
        // We have to use Chan.instance() here because we can't inject it normally since it will
        // create a dependency cycle
        boardRepository = Chan.instance(BoardRepository.class);
        return boardRepository;
    }

    /**
     * Called when the application goes into the background, to do intensive update calls for loadables
     * whose list indexes or titles have changed.
     */
    public Callable<Void> flush() {
        return () -> {
            List<Loadable> toFlush = new ArrayList<>();
            for (Loadable loadable : cachedLoadables.values()) {
                if (loadable.dirty) {
                    loadable.dirty = false;
                    toFlush.add(loadable);
                }
            }

            if (!toFlush.isEmpty()) {
                Logger.d(TAG, "Flushing " + toFlush.size() + " loadable(s)");
                for (Loadable loadable : toFlush) {
                    helper.getLoadableDao().update(loadable);
                }
            }

            return null;
        };
    }

    /**
     * All loadables that are not gotten from a database (like from any of the Loadable.for...() factory methods)
     * need to go through this method to correctly get a loadable if it already existed in the db.
     * <p>It will search the database for existing loadables of the mode is THREAD, and return one of those if there is
     * else it will create the loadable in the database and return the given loadable.
     *
     * @param loadable Loadable to search from that was not yet gotten from the db.
     * @return a loadable ready to use.
     */
    public Loadable getOrCreateLoadable(final Loadable loadable) {
        if (loadable.id != 0) {
            return loadable;
        }

        // We only cache THREAD loadables in the db
        if (loadable.isThreadMode()) {
            return databaseManager.runTask(getLoadable(loadable));
        } else {
            return loadable;
        }
    }

    @Nullable
    public Loadable getByThreadDescriptor(final ChanDescriptor.ThreadDescriptor descriptor) {
        if (loadablesByDescriptors.containsKey(descriptor)) {
            return loadablesByDescriptors.get(descriptor);
        }

        Site site = getSiteRepository().bySiteDescriptor(descriptor.siteDescriptor());
        if (site == null) {
            return null;
        }

        Board board = getBoardRepository().getFromBoardDescriptor(descriptor.getBoardDescriptor());
        if (board == null) {
            return null;
        }

        return databaseManager.runTask(() -> {
            QueryBuilder<Loadable, Integer> builder = helper.getLoadableDao().queryBuilder();
            List<Loadable> results = builder.where()
                    .eq("site", site.id())
                    .and()
                    .eq("mode", Loadable.Mode.THREAD)
                    .and()
                    .eq("board", board.code)
                    .and()
                    .eq("no", descriptor.getThreadNo())
                    .query();

            if (results.size() > 1) {
                Log.w(TAG, "Multiple loadables found for where Loadable.equals() would return true");
                for (Loadable result : results) {
                    Log.w(TAG, result.toString());
                }
            }

            Loadable result = results.isEmpty() ? null : results.get(0);
            if (result == null) {
                return null;
            }

            Log.d(TAG, "Loadable found in db by thread descriptor");
            result.site = site;
            result.board = board;

            cachedLoadables.put(result, result);
            loadablesByDescriptors.put(result.getChanDescriptor(), result);

            return result;
        });
    }

    /**
     * Call this when you use a thread loadable as a foreign object on your table
     * <p>It will correctly update the loadable cache
     *
     * @param loadable Loadable that only has its id loaded
     * @return a loadable ready to use.
     *
     * @throws SQLException database error
     */
    public Loadable refreshForeign(final Loadable loadable)
            throws SQLException {
        if (loadable.id == 0) {
            throw new IllegalArgumentException("This only works loadables that have their id loaded");
        }

        // If the loadable was already loaded in the cache, return that entry
        for (Loadable key : cachedLoadables.keySet()) {
            if (key.id == loadable.id) {
                loadablesByDescriptors.put(key.getChanDescriptor(), loadable);
                return key;
            }
        }

        // Add it to the cache, refresh contents
        helper.getLoadableDao().refresh(loadable);
        loadable.site = getSiteRepository().forId(loadable.siteId);
        loadable.board = loadable.site.board(loadable.boardCode);
        cachedLoadables.put(loadable, loadable);
        return loadable;
    }

    private Callable<Loadable> getLoadable(final Loadable loadable) {
        if (!loadable.isThreadMode()) {
            throw new IllegalArgumentException("getLoadable can only be used for thread loadables");
        }

        return () -> {
            Loadable cachedLoadable = cachedLoadables.get(loadable);
            if (cachedLoadable != null) {
                Logger.v(TAG, "Cached loadable found");

                loadablesByDescriptors.put(cachedLoadable.getChanDescriptor(), cachedLoadable);
                return cachedLoadable;
            } else {
                QueryBuilder<Loadable, Integer> builder = helper.getLoadableDao().queryBuilder();
                List<Loadable> results = builder.where()
                        .eq("site", loadable.siteId)
                        .and()
                        .eq("mode", loadable.mode)
                        .and()
                        .eq("board", loadable.boardCode)
                        .and()
                        .eq("no", loadable.no)
                        .query();

                if (results.size() > 1) {
                    Log.w(TAG, "Multiple loadables found for where Loadable.equals() would return true");
                    for (Loadable result : results) {
                        Log.w(TAG, result.toString());
                    }
                }

                Loadable result = results.isEmpty() ? null : results.get(0);
                if (result == null) {
                    Log.d(TAG, "Creating loadable");
                    helper.getLoadableDao().create(loadable);
                    result = loadable;
                } else {
                    Log.d(TAG, "Loadable found in db");
                    result.site = getSiteRepository().forId(result.siteId);
                    result.board = result.site.board(result.boardCode);
                }

                cachedLoadables.put(result, result);
                loadablesByDescriptors.put(result.getChanDescriptor(), result);

                return result;
            }
        };
    }

    public Callable<List<Loadable>> getLoadables(Site site) {
        return () -> helper.getLoadableDao().queryForEq("site", site.id());
    }

    public Callable<Object> deleteLoadables(List<Loadable> siteLoadables) {
        return () -> {
            Set<Integer> loadableIdSet = new HashSet<>();

            for (Loadable loadable : siteLoadables) {
                loadableIdSet.add(loadable.id);
            }

            DeleteBuilder<Loadable, Integer> builder = helper.getLoadableDao().deleteBuilder();
            builder.where().in("id", loadableIdSet);

            int deletedCount = builder.delete();
            if (loadableIdSet.size() != deletedCount) {
                throw new IllegalStateException("Deleted count didn't equal loadableIdSet.size(). " +
                        "(deletedCount = " + deletedCount + "), " +
                        "(loadableIdSet = " + loadableIdSet.size() + ")");
            }

            return null;
        };
    }

    public Callable<Void> updateLoadable(Loadable updatedLoadable) {
        return () -> {
            for (Loadable key : cachedLoadables.keySet()) {
                if (key.id == updatedLoadable.id) {
                    loadablesByDescriptors.put(key.getChanDescriptor(), updatedLoadable);
                    cachedLoadables.put(key, updatedLoadable);
                    break;
                }
            }

            helper.getLoadableDao().update(updatedLoadable);
            return null;
        };
    }
}
