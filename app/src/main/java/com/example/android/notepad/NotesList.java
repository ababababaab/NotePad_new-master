package com.example.android.notepad;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;



public class NotesList extends ListActivity {

    private static final String TAG = "NotesList";

    private static final String[] PROJECTION = new String[] {
            NotePad.Notes._ID,
            NotePad.Notes.COLUMN_NAME_TITLE,
            NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
    };

    private static final int COLUMN_INDEX_TITLE = 1;

    private String searchQuery = ""; // 用于保存搜索查询的字符串
    private Cursor currentCursor; // 当前的数据光标


    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);

        Intent intent = getIntent();
        if (intent.getData() == null) {
            intent.setData(NotePad.Notes.CONTENT_URI);
        }

        getListView().setOnCreateContextMenuListener(this);

        // 初始加载所有数据
        currentCursor = managedQuery(
                getIntent().getData(),
                PROJECTION,
                null,
                null,
                NotePad.Notes.DEFAULT_SORT_ORDER
        );

        String[] dataColumns = { NotePad.Notes.COLUMN_NAME_TITLE,
                NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE };
        int[] viewIDs = { android.R.id.text1, R.id.time };

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                this,
                R.layout.noteslist_item,  // 这里引用的布局文件，确保名称和修改后的布局文件名对应
                currentCursor,
                dataColumns,
                viewIDs
        );

        setListAdapter(adapter);
    }

    private void performSort() {
        String sortOrderColumn = SORT_ORDERS[currentSortOrderIndex];
        String sortOrder = isAscendingOrder? sortOrderColumn + " ASC" : sortOrderColumn + " DESC";
        if (TextUtils.isEmpty(searchQuery)) {
            currentCursor = getContentResolver().query(
                    getIntent().getData(),
                    PROJECTION,
                    null,
                    null,
                    sortOrder
            );
        } else {
            String selection = NotePad.Notes.COLUMN_NAME_TITLE + " LIKE?";
            String[] selectionArgs = new String[]{"%" + searchQuery + "%"};

            currentCursor = getContentResolver().query(
                    getIntent().getData(),
                    PROJECTION,
                    selection,
                    selectionArgs,
                    sortOrder
            );
        }

        if (currentCursor!= null && currentCursor.getCount() > 0) {
            SimpleCursorAdapter adapter = (SimpleCursorAdapter) getListAdapter();
            adapter.changeCursor(currentCursor);
        } else {
            Toast.makeText(this, "未找到符合条件的note", Toast.LENGTH_SHORT).show();
        }
    }

    private static final String[] SORT_ORDERS = {NotePad.Notes.COLUMN_NAME_TITLE, NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE};
    private int currentSortOrderIndex = 0; // 初始化为按标题排序，索引对应SORT_ORDERS数组


    private boolean isAscendingOrder = true; // 初始化为升序

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_options_menu, menu);

        // 添加日志输出，检查是否进入到排序菜单项创建的逻辑
        Log.d(TAG, "Creating sort menu item");

        // 添加排序菜单项并正确定义sortMenuItem变量
        MenuItem sortMenuItem = menu.add("排序");
        // 添加日志输出，检查图标设置代码是否执行
        Log.d(TAG, "Setting icon for sort menu item");
        sortMenuItem.setIcon(android.R.drawable.ic_menu_sort_by_size);
        sortMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // 切换排序方式
                currentSortOrderIndex = (currentSortOrderIndex + 1) % SORT_ORDERS.length;
                performSort();
                return true;
            }
        });

        // 为搜索菜单项添加一个替代菜单项（原有的逻辑保持不变）
        Intent intent = new Intent(null, getIntent().getData());
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        MenuItem mPasteItem = menu.findItem(R.id.menu_paste);
        if (clipboard.hasPrimaryClip()) {
            mPasteItem.setEnabled(true);
        } else {
            mPasteItem.setEnabled(false);
        }

        final boolean haveItems = getListAdapter().getCount() > 0;
        if (haveItems) {
            Uri uri = ContentUris.withAppendedId(getIntent().getData(), getSelectedItemId());
            Intent[] specifics = new Intent[1];
            specifics[0] = new Intent(Intent.ACTION_EDIT, uri);

            MenuItem[] items = new MenuItem[1];
            Intent intent = new Intent(null, uri);
            intent.addCategory(Intent.CATEGORY_ALTERNATIVE);

            menu.addIntentOptions(
                    Menu.CATEGORY_ALTERNATIVE,
                    Menu.NONE,
                    Menu.NONE,
                    null,
                    specifics,
                    intent,
                    Menu.NONE,
                    items
            );

            if (items[0] != null) {
                items[0].setShortcut('1', 'e');
            }
        } else {
            menu.removeGroup(Menu.CATEGORY_ALTERNATIVE);
        }

        return true;
    }
    // 将菜单相关资源ID声明为final静态常量
    private static final int MENU_ADD = R.id.menu_add;
    private static final int MENU_PASTE = R.id.menu_paste;
    private static final int MENU_SEARCH = R.id.menu_search;

    // 其他类成员变量和方法


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == MENU_ADD) {
            startActivity(new Intent(Intent.ACTION_INSERT, getIntent().getData()));
            return true;
        } else if (item.getItemId() == MENU_PASTE) {
            startActivity(new Intent(Intent.ACTION_PASTE, getIntent().getData()));
            return true;
        } else if (item.getItemId() == MENU_SEARCH) {
            startSearchActivity();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void startSearchActivity() {
        // 弹出输入框，用户输入查询内容
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("请输入搜索内容");

        // 设置输入框
        final EditText input = new EditText(this);
        builder.setView(input);

        // 设置确定按钮
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 获取用户输入的查询内容
                searchQuery = input.getText().toString();
                performSearch();
            }
        });

        // 设置取消按钮
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.show();
    }

    private void performSearch() {

        if (TextUtils.isEmpty(searchQuery)) {

            currentCursor = getContentResolver().query(
                    getIntent().getData(),
                    PROJECTION,
                    null, // 不需要过滤条件
                    null,
                    NotePad.Notes.DEFAULT_SORT_ORDER
            );
        } else {

            String selection = NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ?";
            String[] selectionArgs = new String[]{"%" + searchQuery + "%"};

            currentCursor = getContentResolver().query(
                    getIntent().getData(),
                    PROJECTION,
                    selection,
                    selectionArgs,
                    NotePad.Notes.DEFAULT_SORT_ORDER
            );
        }

        // 如果查询结果不为空，刷新列表
        if (currentCursor != null && currentCursor.getCount() > 0) {
            SimpleCursorAdapter adapter = (SimpleCursorAdapter) getListAdapter();
            adapter.changeCursor(currentCursor); // 更新数据源
        } else {
            // 如果没有找到任何结果，显示提示
            Toast.makeText(this, "未找到符合条件的note", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return;
        }

        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
        if (cursor == null) {
            return;
        }

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_context_menu, menu);

        menu.setHeaderTitle(cursor.getString(COLUMN_INDEX_TITLE));

        Intent intent = new Intent(null, Uri.withAppendedPath(getIntent().getData(),
                Integer.toString((int) info.id)));
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return false;
        }

        Uri noteUri = ContentUris.withAppendedId(getIntent().getData(), info.id);

        if (item.getItemId() == R.id.context_open) {
            startActivity(new Intent(Intent.ACTION_EDIT, noteUri));
            return true;
        } else if (item.getItemId() == R.id.context_copy) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newUri(
                    getContentResolver(),
                    "Note",
                    noteUri)
            );
            return true;
        } else if (item.getItemId() == R.id.context_delete) {
            getContentResolver().delete(
                    noteUri,
                    null,
                    null
            );
            return true;
        }
        return super.onContextItemSelected(item);
    }
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);
        String action = getIntent().getAction();

        if (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action)) {
            setResult(RESULT_OK, new Intent().setData(uri));
        } else {
            startActivity(new Intent(Intent.ACTION_EDIT, uri));
        }
    }
}
