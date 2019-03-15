package xyz.hiroshifuu.speechapp.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import io.github.ponnamkarthik.richlinkpreview.MetaData;
import io.github.ponnamkarthik.richlinkpreview.ResponseListener;
import io.github.ponnamkarthik.richlinkpreview.RichLinkView;
import io.github.ponnamkarthik.richlinkpreview.RichPreview;
import io.github.ponnamkarthik.richlinkpreview.ViewListener;
import xyz.hiroshifuu.speechapp.R;
import xyz.hiroshifuu.speechapp.common.SpeechItem;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class CustomAdapter extends ArrayAdapter<SpeechItem> {

    private ArrayList<SpeechItem> Items;
    Context mContext;
    private String main_url;

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
        else {
            if (!item.isMap())
                convertView = inflater.inflate(R.layout.list_item_res, parent, false);
            else {
                convertView = inflater.inflate(R.layout.list_item_map, parent, false);
                final RichLinkView richLinkView = (RichLinkView) convertView.findViewById(R.id.richLinkView);
                richLinkView.setLink(item.getText(), new ViewListener() {

                    @Override
                    public void onSuccess(boolean status) {

                    }

                    @Override
                    public void onError(Exception e) {

                    }
                });
                convertView = inflater.inflate(R.layout.list_item_map2, parent, false);
                main_url = item.getText();
                final LinearLayout linearLayout = (LinearLayout) convertView.findViewById(R.id.rich_link_card);
                final ImageView imageView = (ImageView) convertView.findViewById(R.id.rich_link_image);
                final TextView textViewTitle = (TextView) convertView.findViewById(R.id.rich_link_title);
                final TextView textViewDesp = (TextView) convertView.findViewById(R.id.rich_link_desp);
                RichPreview richPreview = new RichPreview(new ResponseListener() {
                    @Override
                    public void onData(MetaData metaData) {
                        MetaData meta = metaData;

                        if(meta.getImageurl().equals("") || meta.getImageurl().isEmpty()) {
                            imageView.setVisibility(GONE);
                        } else {
                            imageView.setVisibility(VISIBLE);
                            Picasso.get()
                                    .load(meta.getImageurl())
                                    .into(imageView);
                        }

                        if(meta.getTitle().isEmpty() || meta.getTitle().equals("")) {
                            textViewTitle.setVisibility(GONE);
                            Log.d("getTitle", meta.getTitle());
                        } else {
                            textViewTitle.setVisibility(VISIBLE);
                            textViewTitle.setText(meta.getTitle());
                        }
                        if(meta.getDescription().isEmpty() || meta.getDescription().equals("")) {
                            textViewDesp.setVisibility(GONE);
                            Log.d("getDescription", meta.getDescription());
                        } else {
                            textViewDesp.setVisibility(VISIBLE);
                            textViewDesp.setText(meta.getDescription());
                        }

                        linearLayout.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                richLinkClicked();
                            }
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        //handle error
                    }
                });
                richPreview.getPreview(main_url);
            }
        }
        if (!item.isMap()) {
            final TextView tv =  convertView.findViewById(R.id.textview);
//            tv.setClickable(true);
//            tv.setMovementMethod(LinkMovementMethod.getInstance());
//            tv.setText(Html.fromHtml(item.getText()));
            tv.setText(item.getText());
        }
        return convertView;
    }

    private void richLinkClicked() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(main_url));
        mContext.startActivity(intent);
    }
}
