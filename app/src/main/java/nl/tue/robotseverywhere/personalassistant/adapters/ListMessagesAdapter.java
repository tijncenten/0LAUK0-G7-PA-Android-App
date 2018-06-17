package nl.tue.robotseverywhere.personalassistant.adapters;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import nl.tue.robotseverywhere.personalassistant.R;
import pma.PersonalMessagingAssistant;
import pma.message.Message;

public class ListMessagesAdapter extends RecyclerView.Adapter<ListMessagesAdapter.MyViewHolder> {
    private List<Message> mDataset = new ArrayList<>();

    private boolean hideSpam = true;

    public static class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public ConstraintLayout mConstraintLayout;

        public MyViewHolder(ConstraintLayout v) {
            super(v);
            v.setOnClickListener(this);
            mConstraintLayout = v;
        }

        @Override
        public void onClick(View v) {
        }

        public void setVisible(boolean visible) {
            RecyclerView.LayoutParams param = (RecyclerView.LayoutParams)itemView.getLayoutParams();
            if (visible) {
                mConstraintLayout.setVisibility(View.VISIBLE);
                param.height = ConstraintLayout.LayoutParams.WRAP_CONTENT;
                param.width = ConstraintLayout.LayoutParams.MATCH_PARENT;
            } else {
                mConstraintLayout.setVisibility(View.GONE);
                param.height = 0;
                param.width = 0;
            }
            itemView.setLayoutParams(param);
        }

    }

    public ListMessagesAdapter(List<Message> myDataset) {
        mDataset = myDataset;
    }

    public ListMessagesAdapter() {

    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ConstraintLayout v = (ConstraintLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.list_message, parent, false);

        MyViewHolder vh = new MyViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        TextView threadId = (TextView) holder.mConstraintLayout.findViewById(R.id.list_message_thread);
        View spamIndicator = holder.mConstraintLayout.findViewById(R.id.list_message_spam_indicator);
        TextView sender = (TextView) holder.mConstraintLayout.findViewById(R.id.list_message_sender);
        TextView text = (TextView) holder.mConstraintLayout.findViewById(R.id.list_message_text);
        TextView time = (TextView) holder.mConstraintLayout.findViewById(R.id.list_message_time);
        View background = holder.mConstraintLayout.findViewById(R.id.list_message_background);

        Message m = mDataset.get(position);

        if (hideSpam && m.hasResult() && m.getResult() == PersonalMessagingAssistant.EvalResult.low) {
            holder.setVisible(false);
        } else {
            holder.setVisible(true);
        }

        threadId.setText(Integer.toString(m.getThreadIndex()));

        if (m.isSpam() != null) {
            if (m.isSpam()) {
                spamIndicator.setBackgroundResource(R.color.colorEvalLow);
            } else {
                spamIndicator.setBackgroundResource(R.color.colorEvalHigh);
            }
        } else {
            spamIndicator.setBackgroundResource(R.color.colorEvalNormal);
        }

        if (m.hasResult()) {
            switch (m.getResult()) {
                case high:
                    background.setBackgroundResource(R.color.colorEvalHigh);
                    break;
                case medium:
                    background.setBackgroundResource(R.color.colorEvalMedium);
                    break;
                case low:
                    background.setBackgroundResource(R.color.colorEvalLow);
                    break;
                default:
                    background.setBackgroundResource(R.color.colorEvalNormal);
                    break;
            }
        } else {
            background.setBackgroundResource(R.color.colorEvalNormal);
        }

        sender.setText(m.getSender().toString() + ":");
        text.setText(m.getOriginalText());

        Date d = new Date(m.getTimestamp());
        DateFormat f = new SimpleDateFormat("dd-MM-yy HH:mm");

        time.setText(f.format(d));
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public List<Message> getData() {
        return mDataset;
    }

    public void setData(List<Message> messages) {
        mDataset = messages;
        notifyDataSetChanged();
    }

    public void setResults(PersonalMessagingAssistant.EvalResult[] results) {
        if (results.length != mDataset.size()) {
            throw new IllegalStateException("size not equal");
        }

        for (int i = 0; i < results.length; i++) {
            mDataset.get(i).setResult(results[i]);
        }
        notifyDataSetChanged();
    }

    public void setThreads(int[] threadIndices) {
        if (threadIndices.length != mDataset.size()) {
            throw new IllegalStateException("size not equal");
        }

        for (int i = 0; i < threadIndices.length; i++) {
            mDataset.get(i).setThreadIndex(threadIndices[i]);
        }
        notifyDataSetChanged();
    }

    public void setHideSpam(boolean value) {
        this.hideSpam = value;
        notifyDataSetChanged();
    }

    public void addMessage(Message m) {
        mDataset.add(m);
        notifyItemInserted(mDataset.size()-1);
    }
}
