package com.reviewbetterbonia.app.ui;
import androidx.fragment.app.Fragment;import com.reviewbetterbonia.app.MainActivity;
public abstract class BaseFragment extends Fragment { protected MainActivity app(){return (MainActivity)requireActivity();} }
