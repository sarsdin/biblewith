package com.example.androidclient.bible;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.example.androidclient.R;
import com.example.androidclient.databinding.BibleVerseFmVhBinding;

import java.util.List;

public class BibleVerseRva extends RecyclerView.Adapter<BibleVerseRva.BibleVerseVh> {

    private BibleVm bibleVm;
    public BibleVerseFm bibleVerseFm;
    private List<BibleDto> list;
    private int position; //뷰홀더 포지션(위치)

    public BibleVerseRva(BibleVm bibleVm, BibleVerseFm bibleVerseFm) {
        this.bibleVm = bibleVm;
        this.bibleVerseFm = bibleVerseFm;
    }

    @Override
    public BibleVerseVh onCreateViewHolder(ViewGroup parent, int viewType) {
//        Log.e("[BibleVerseRva]", "onCreateViewHolder "+ holderSize++);
        BibleVerseFmVhBinding binding = BibleVerseFmVhBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);

        return new BibleVerseVh(binding);
    }

    @Override
    public void onBindViewHolder(final BibleVerseVh holder, int position) {
        int safePosition = holder.getBindingAdapterPosition();
        holder.bind(bibleVm.verseL.get(safePosition));
//        Log.e("[BibleVerseRva]", "onBindViewHolder "+ holder.getAbsoluteAdapterPosition());
    }

    @Override
    public int getItemCount() {
        return bibleVm.verseL.size();
    }

    public class BibleVerseVh extends RecyclerView.ViewHolder {
        public final TextView verseNo;
        public final TextView verseContent;
        public BibleDto mItem;
        BibleVerseFmVhBinding binding;

        public BibleVerseVh(BibleVerseFmVhBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            verseNo = binding.verseNo;
            verseContent = binding.verseContent;
        }


        public void bind(BibleDto mItem) {
            this.mItem = mItem;
            binding.verseNo.setText(String.valueOf(mItem.getVerse()));
            binding.verseContent.setText(mItem.getContent());


            binding.getRoot().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                    position = getAbsoluteAdapterPosition(); //현재 홀더의 절대위치 LayoutManager를 이용하는 기능이라면 이것의 위치를 사용해야한다.
                    position = getLayoutPosition();
                    if (position != RecyclerView.NO_POSITION   ) {
                        bibleVm.절번호업데이트( mItem.getVerse() );
                        Navigation.findNavController(v).navigate(R.id.action_global_myHighLightBts);

                        //click event 등 안에 갱신 명령어가 있으면 Cannot call this method while RecyclerView is computing a layout 이 뜨지 않는가?
                        // onbindviewholder() 메소드안에 실행되는 notifyDataSetChanged(); 를 주석처리하면 안뜸.
                        notifyDataSetChanged();

                    }
                }
            });

        }
    }
}