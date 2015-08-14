package com.volcano.persistentcollection;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.volcano.persistent.PersistentQueue;
import com.volcano.persistent.XQueue;

import java.util.NoSuchElementException;

/**
 * Main activity
 */
public final class MainActivity extends Activity {
    private XQueue mQueue;

    private TextView mLogText;
    private EditText mPositionEdit;
    private EditText mInputEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mQueue = PersistentQueue.getInstance(this);

        final Button enqueueButton = (Button) findViewById(R.id.button_enqueue);
        final Button dequeueButton = (Button) findViewById(R.id.button_dequeue);
        final Button peekButton = (Button) findViewById(R.id.button_peek);
        final Button peekAtButton = (Button) findViewById(R.id.button_peek_at);
        final Button insertAtButton = (Button) findViewById(R.id.button_insert_at);
        final Button removeFromButton = (Button) findViewById(R.id.button_remove_from);
        final Button getSizeButton = (Button) findViewById(R.id.button_get_size);
        final Button isEmptyButton = (Button) findViewById(R.id.button_is_empty);
        mInputEdit = (EditText) findViewById(R.id.edit_value);
        mPositionEdit = (EditText) findViewById(R.id.edit_position);
        mLogText = (TextView) findViewById(R.id.text_log);
        mLogText .setMovementMethod(new ScrollingMovementMethod());
        mLogText.setText("Log area...", TextView.BufferType.EDITABLE);

        enqueueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String input = mInputEdit.getText().toString();
                if (!TextUtils.isEmpty(input)) {
                    mQueue.enqueue(input);
                    addLog("'" + input + "' added to queue");
                }
                else {
                    showToast("Insert item");
                }
            }
        });

        dequeueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String item = mQueue.dequeue();
                if (item != null) {
                    addLog("First item removed from queue");
                }
                else {
                    addLog("The queue is empty");
                }
            }
        });

        peekButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String item = mQueue.peek();
                if (item != null) {
                    addLog(item);
                }
                else {
                    addLog("The queue is empty");
                }
            }
        });

        peekAtButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int position = getPosition();
                if (position != -1) {
                    try {
                        final String item = mQueue.peekAt(position);
                        if (item != null) {
                            addLog(item);
                        }
                        else {
                            addLog("The queue is empty");
                        }
                        mPositionEdit.setText(null);
                    }
                    catch (IndexOutOfBoundsException e) {
                        addLog("Position is out of range");
                    }
                }
                else {
                    showToast("Insert position");
                }
            }
        });

        insertAtButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String input = mInputEdit.getText().toString();
                final int position = getPosition();
                if (!TextUtils.isEmpty(input) && position != -1) {
                    try {
                        mQueue.insertAt(position, input);
                        addLog("'" + input + "' added at position " + position);
                    } catch (IndexOutOfBoundsException e) {
                        addLog("Position is out of range");
                    }
                }
                else {
                    showToast("Insert inputs");
                }
            }
        });

        removeFromButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int position = getPosition();
                if (position != -1) {
                    try {
                        mQueue.removeAt(position);
                        addLog("Removed item from position " + position);
                    }
                    catch (IndexOutOfBoundsException e) {
                        addLog("Position is out of range");
                    }
                    catch (NoSuchElementException e) {
                        addLog("The queue is empty");
                    }
                }
                else {
                    showToast("Insert position");
                }
            }
        });

        getSizeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addLog("The queue size is " + mQueue.size());
            }
        });

        isEmptyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addLog("The queue is " + (mQueue.isEmpty() ? "empty" : "not empty"));
            }
        });
    }

    private void addLog(String message) {
        ((Editable) mLogText.getText()).insert(0, message + "\n");
    }

    private int getPosition() {
        final String positionText = mPositionEdit.getText().toString();
        int position;
        if (!TextUtils.isEmpty(positionText)) {
            position = Integer.valueOf(mPositionEdit.getText().toString());
        }
        else {
            position = -1;
        }
        return position;
    }

    private void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}
