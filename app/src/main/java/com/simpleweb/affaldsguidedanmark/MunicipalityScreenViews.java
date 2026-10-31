package com.simpleweb.affaldsguidedanmark;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

final class MunicipalityScreenViews {
    private MunicipalityScreenViews() {}
    static int dp(Context context, int value) { return Math.round(value * context.getResources().getDisplayMetrics().density); }
    static String text(Context context, String key, String... args) {
        String text = InfoPageText.get(context, key);
        for (int i = 0; i < args.length; i++) text = text.replace("{" + i + "}", args[i]);
        return text;
    }
    static TextView label(Context context, String text, int size, boolean bold, boolean secondary) {
        TextView view = new TextView(context);
        view.setText(text); view.setTextSize(size);
        view.setTextColor(ContextCompat.getColor(context, secondary ? R.color.grey_black : R.color.text_color));
        if (bold) view.setTypeface(view.getTypeface(), Typeface.BOLD);
        return view;
    }
    static LinearLayout page(Fragment fragment, String title) {
        Context context = fragment.requireContext();
        LinearLayout page = new LinearLayout(context); page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(ContextCompat.getColor(context, R.color.page_background));
        LinearLayout header = new LinearLayout(context); header.setGravity(Gravity.CENTER_VERTICAL);
        ImageButton back = new ImageButton(context); back.setImageResource(R.drawable.arrow_left);
        back.setColorFilter(ContextCompat.getColor(context, R.color.green_light)); back.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        back.setContentDescription(context.getString(R.string.onboarding_back));
        back.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        header.addView(back, new LinearLayout.LayoutParams(dp(context,48),dp(context,56)));
        TextView heading = label(context,title,21,true,false);
        heading.setPadding(0,dp(context,12),dp(context,16),dp(context,12));
        header.addView(heading,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1));
        page.addView(header);
        return page;
    }
    static EditText search(Context context, String hint, java.util.function.Consumer<String> changed) {
        EditText input = new EditText(context); input.setSingleLine(true); input.setTextSize(16); input.setHint(hint);
        input.setTextColor(ContextCompat.getColor(context,R.color.text_color));
        input.setBackgroundResource(R.drawable.search_group_background);
        input.setPadding(dp(context,14),dp(context,10),dp(context,14),dp(context,10));
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1,dp(context,50));
        params.setMargins(dp(context,16),dp(context,4),dp(context,16),dp(context,12)); input.setLayoutParams(params);
        input.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after) {}
            public void onTextChanged(CharSequence s,int start,int before,int count) { changed.accept(s.toString()); }
            public void afterTextChanged(Editable value) {}
        });
        return input;
    }
    static LinearLayout card(Context context) {
        LinearLayout card = new LinearLayout(context); card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(context,16),dp(context,14),dp(context,16),dp(context,14));
        card.setBackgroundResource(R.drawable.search_group_background);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1,-2);
        params.setMargins(dp(context,16),0,dp(context,16),dp(context,10)); card.setLayoutParams(params);
        card.setClickable(true); card.setFocusable(true);
        android.util.TypedValue ripple = new android.util.TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground,ripple,true);
        card.setForeground(ContextCompat.getDrawable(context,ripple.resourceId));
        return card;
    }
}
