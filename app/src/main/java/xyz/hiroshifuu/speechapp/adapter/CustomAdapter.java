package xyz.hiroshifuu.speechapp.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import xyz.hiroshifuu.speechapp.R;
import xyz.hiroshifuu.speechapp.common.SpeechItem;

public class CustomAdapter extends ArrayAdapter<SpeechItem> {

    private ArrayList<SpeechItem> Items;
    Context mContext;

    public CustomAdapter(ArrayList<SpeechItem> Items, Context context) {
        super(context, R.layout.list_item, Items);
        this.Items = Items;
        this.mContext = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        SpeechItem item = getItem(position);

        LayoutInflater inflater = LayoutInflater.from(getContext());
        if (item.isReq())
            convertView = inflater.inflate(R.layout.list_item_req, parent, false);
        else
            convertView = inflater.inflate(R.layout.list_item_res, parent, false);

        final TextView tv =  convertView.findViewById(R.id.textview);
        tv.setText(item.getText());

        return convertView;
    }
}
