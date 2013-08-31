
package org.gjt.sp.jedit.options;

import javax.swing.*;

class IntegerInputVerifier extends InputVerifier
{
    @Override
    public boolean verify(JComponent input)
    {
        if (! (input instanceof JTextField))
            return true;
        JTextField tf = (JTextField) input;
        int i;
        try
        {
            i = Integer.valueOf(tf.getText()).intValue();
        }
        catch (Exception e)
        {
            return false;
        }
        return (i >= 0);
    }
};

