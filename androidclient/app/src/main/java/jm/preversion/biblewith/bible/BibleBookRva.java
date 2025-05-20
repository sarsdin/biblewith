package jm.preversion.biblewith.bible;

import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import jm.preversion.biblewith.MyApp;
import jm.preversion.biblewith.R;
import jm.preversion.biblewith.bible.dto.BibleDto;
import jm.preversion.biblewith.databinding.BibleBookFmBookVhBinding;
import jm.preversion.biblewith.databinding.BibleFmBinding;

import java.util.List;

//viewType 을 쓰고 싶다면 <RecyclerView.ViewHolder> 를 상속받아야한다. 특정 뷰홀더를 받지않음. 나중에 캐스팅하여 사용.
public class BibleBookRva extends RecyclerView.Adapter<BibleBookRva.BibleBookVh> {

//    interface 뷰페이저어뎁터컨트롤리스너{
//        BibleFmBinding 바인딩가져오기();
//    }
//    private 뷰페이저어뎁터컨트롤리스너 listener;

    private BibleVm bibleVm;
    private BibleFm bibleFm;
    private BibleBookFm bibleBookFm;
    public BibleFmBinding bibleFmBinding;
    private List<BibleDto> list;
    private int position; //뷰홀더 포지션(위치)

    public BibleBookRva(BibleVm vm, BibleBookFm fm) {
        this.bibleVm = vm;
        this.bibleBookFm = fm;
    }

    int holderSize = 0;
    @Override
    public BibleBookVh onCreateViewHolder(ViewGroup parent, int viewType) {
//        Log.e("[BibleBookRav]", "onCreateViewHolder "+ holderSize++);
        BibleBookFmBookVhBinding binding = BibleBookFmBookVhBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);

        return new BibleBookVh(binding);

    }

    @SuppressLint("ResourceAsColor")
    @Override
    public void onBindViewHolder(BibleBookVh holder, int position) {
        holder.bind(bibleVm.bookL.get(position));
//        Log.e("[BibleBookRav]", "onBindViewHolder "+ holder.getAbsoluteAdapterPosition());

    }

    @Override
    public int getItemCount() {
        return bibleVm.bookL.size();
//        return mValues.size();
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }


    @Override
    public void onViewAttachedToWindow(@NonNull BibleBookVh holder) {
        holder.setIsRecyclable(false);
        super.onViewAttachedToWindow(holder);
//        Log.e("[BibleBookRav]", "onViewAttachedToWindow "+ holder.getAbsoluteAdapterPosition());
    }



    public class BibleBookVh extends RecyclerView.ViewHolder {
        public final TextView bookName;
        public final TextView bookCategory;
        public BibleDto mItem;
        BibleBookFmBookVhBinding binding;

        public BibleBookVh(BibleBookFmBookVhBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            bookName = binding.bookName;
            bookCategory = binding.bookCategory;
        }

        @SuppressLint("ResourceAsColor")
        public void bind(BibleDto mItem){
            this.mItem = mItem;
            binding.bookName.setText(mItem.getBook_name());
            binding.bookCategory.setText(mItem.getBook_category());
            if (mItem.getCurrentItem()) {
                binding.bookName.setTypeface(Typeface.DEFAULT_BOLD);
                binding.bookName.setTextColor(MyApp.getApplication().getColorStateList(R.color.book_rv));
                binding.bookCategory.setTypeface(Typeface.DEFAULT_BOLD);
                binding.bookCategory.setTextColor(MyApp.getApplication().getColorStateList(R.color.book_rv));
            }

            //홀더 클릭시 - 선택된 홀더 아이템의 데이터(성경책이름)을 vm의 책장번호 배열로 저장해야한다.
            binding.getRoot().setOnClickListener(new View.OnClickListener() {
                @SuppressLint("ResourceAsColor")
                @Override
                public void onClick(View v) {
//                    Log.e("[BibleBookRav]", "책장번호[0]: " + bibleVm.책장번호[0] ); //책장번호업데이트전 번호
//                    position = getAbsoluteAdapterPosition(); //현재 홀더의 절대위치 LayoutManager를 이용하는 기능이라면 이것의 위치를 사용해야한다.
                    position = getLayoutPosition();
                    if (position != RecyclerView.NO_POSITION   ) {
                        //로딩바 보이기
                        ((BibleVerseFm) ( bibleBookFm.getParentFragmentManager().findFragmentById(0))).binding.bibleVerseFmProgressbar.setVisibility(View.VISIBLE);
                        bibleVm.책장번호업데이트( mItem.getBook()); //position == mItem.book-1  -- position 은 0부터 인덱스가 시작이기 때문 book은 창세기가 1부터 시작임.

                        Log.e("[BibleBookRav]", "position 클릭시: "+ position + ", "+ bibleVm.책장번호[0] +" , "+ mItem.getBook()+" , "+ mItem.getBook_name()+ " , "+ mItem.getCurrentItem() );
//                        listener.바인딩가져오기().bibleTabLayoutViewpager.setCurrentItem(1); //장 페이지로 넘김  ..인터페이스 작동x
//                        bibleFmBinding.bibleTabLayoutViewpager.setCurrentItem(1); x
//                        Navigation.findNavController(v).getContext().getPackageManager(). x
//                        bibleBookFm.getParentFragmentManager().findFragmentById()

                        bibleVm.장목록가져오기(bibleVm.책장번호[0], "BibleBookVh");
                        bibleVm.절목록가져오기(bibleVm.책장번호[0], bibleVm.책장번호[1]);
                        //홀더 클릭시 장탭으로 넘어가기
                        assert bibleBookFm.getParentFragment() != null;
                        ((BibleFm) bibleBookFm.getParentFragment()).binding.bibleTabLayoutViewpager.setCurrentItem(1);
                        //스크롤 제일 위로 가기
                        ((BibleVerseFm) ( bibleBookFm.getParentFragmentManager().findFragmentById(0))).recyclerView.scrollToPosition(0);
//                        ((BibleVerseFm) ( bibleChapterFm.getParentFragmentManager().getFragments().get(2))).recyclerView.scrollToPosition(0);
                        //현재 보고있는 화면의 프라그먼트의 id를 가져오기
//                        Log.e("[BibleBookRav]", "getParentFragmentManager: " +  bibleChapterFm.getParentFragmentManager().getFragments().get(0).getId() );
                        //id가 아닌 findFragmentByTag("f2") 로 가져오고 싶은 경우 viewpager2에서는 "f0", "f1", "f2" 등으로 각 인덱스에 f 가 붙은 태그네임이 디폴트로 잡혀있다.

                        notifyDataSetChanged();

                    }
                }
            });


        }

    }



//    @Override
//    public void onViewDetachedFromWindow(@NonNull BibleBookVh holder) {
////        holder.setIsRecyclable(true);
//        super.onViewDetachedFromWindow(holder);
//        Log.e("[BibleBookRav]", "onViewDetachedFromWindow "+ holder.getAbsoluteAdapterPosition());
//    }
//
//    @Override
//    public void onViewRecycled(@NonNull BibleBookVh holder) {
//        super.onViewRecycled(holder);
//        Log.e("[BibleBookRav]", "onViewRecycled "+ holder.getAbsoluteAdapterPosition());
//    }
//
//    @Override
//    public boolean onFailedToRecycleView(@NonNull BibleBookVh holder) {
//        Log.e("[BibleBookRav]", "onFailedToRecycleView "+ holder.getAbsoluteAdapterPosition());
//        return super.onFailedToRecycleView(holder);
//    }
//
//    @Override
//    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
//        super.onAttachedToRecyclerView(recyclerView);
//        Log.e("[BibleBookRav]", "onAttachedToRecyclerView ");
//    }
//
//    @Override
//    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
//        super.onDetachedFromRecyclerView(recyclerView);
//        Log.e("[BibleBookRav]", "onDetachedFromRecyclerView ");
//    }
}