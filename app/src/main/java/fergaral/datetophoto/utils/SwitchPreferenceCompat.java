package fergaral.datetophoto.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.preference.CheckBoxPreference;
import android.preference.TwoStatePreference;
import android.support.v7.widget.SwitchCompat;
import android.util.AttributeSet;
import android.view.View;

import fergaral.datetophoto.R;

/**
 * Created by fer on 25/06/15.
 */
public class SwitchPreferenceCompat extends CheckBoxPreference {

    private SwitchCompat mSwitch;
    private static final boolean DEFAULT_VALUE = false;

    public SwitchPreferenceCompat(Context context) {
        super(context);
        init();
    }

    public SwitchPreferenceCompat(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @TargetApi(21)
    public SwitchPreferenceCompat(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public SwitchPreferenceCompat(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setWidgetLayoutResource(R.layout.switch_preference_layout);
    }

    //Este código es por si no ponemos el id del SwitchCompat como checkbox (@android:id/checkbox)
    //y si no ponemos clickable="false" y focusable="false"
    /*@Override
    protected void onBindView(View view) {
        super.onBindView(view);

        SwitchCompat switchView = (SwitchCompat) view.findViewById(R.id.switchpref);

        if(switchView != null) {
            switchView.setChecked(isChecked());
        }
    }*/
}
