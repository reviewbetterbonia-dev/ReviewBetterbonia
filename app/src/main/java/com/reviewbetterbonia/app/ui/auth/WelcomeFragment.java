package com.reviewbetterbonia.app.ui.auth;
import android.content.Context;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.Nullable;

import com.reviewbetterbonia.app.MainActivity;
import com.reviewbetterbonia.app.R;
import com.reviewbetterbonia.app.ui.*;

public class WelcomeFragment extends BaseFragment {
 @Nullable public View onCreateView(LayoutInflater i, ViewGroup c, Bundle b) {
     Context x = requireContext();
     LinearLayout p = Ui.page(x);
     Space top = new Space(x);
     Ui.add(p, top, Ui.dp(x, 30));

     ImageView logo = new ImageView(x);
     logo.setImageResource(R.mipmap.ic_launcher_foreground);
     logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
     LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(Ui.dp(x, 96), Ui.dp(x, 96));
     logoParams.gravity = Gravity.CENTER;
     p.addView(logo, logoParams);

     Space midSpace = new Space(x);
     Ui.add(p, midSpace, Ui.dp(x, 20));

     TextView brand = Ui.text(x, "REVIEW <b>BETTERBONIA</b>", 34, true);
     brand.setGravity(Gravity.CENTER);
     Ui.add(p, brand, Ui.dp(x, 85));
     TextView sub = Ui.muted(x, "Study like a kid with big dreams", 15);
     sub.setGravity(Gravity.CENTER);
     Ui.add(p, sub, Ui.dp(x, 48));

     Ui.addWeight(p, new Space(x));

     Button login = Ui.button(x, "Log in", true);
     login.setOnClickListener(v -> ((MainActivity)requireActivity()).navigate(new AuthFragment(false), true));
     Ui.add(p, login, Ui.dp(x, 56));
     Ui.add(p, Ui.gap(x, 10), Ui.dp(x, 10));

     Button reg = Ui.button(x, "Create account", false);
     reg.setOnClickListener(v -> ((MainActivity)requireActivity()).navigate(new AuthFragment(true), true));
     Ui.add(p, reg, Ui.dp(x, 56));
     Ui.add(p, Ui.gap(x, 10), Ui.dp(x, 10));

     return p;
 }
}
