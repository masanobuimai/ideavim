package com.maddyhome.idea.vim.action.change.insert;

/*
 * IdeaVim - A Vim emulator plugin for IntelliJ Idea
 * Copyright (C) 2003-2005 Rick Maddy
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.maddyhome.idea.vim.group.CommandGroups;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Locale;

/**
 */
public class InsertExitModeAction extends EditorAction {
  public InsertExitModeAction() {
    super(new Handler());
  }

  private static class Handler extends EditorActionHandler {
    private interface IMControl { void disable(JComponent component); }

    private IMControl imControl = new IMControl() {
        // デフォルトは何もしない
        @Override public void disable(JComponent component) { }
    };

    Handler() {
      String imControlflag = System.getProperty("ideavim.imcontrol", "");
      if (imControlflag.equalsIgnoreCase("windows")) {          // Windowsの場合
        imControl = new IMControl() {
          @Override public void disable(JComponent component) {
            try {
              // 強制的にInput MethodをOFFにする
              // （ここがコケても影響ないようにtryブロックに納める）
              component.enableInputMethods(false);
              component.getInputContext().setCompositionEnabled(false);
            } catch (Exception ignore) {}
          }
        };
      } else if (imControlflag.equalsIgnoreCase("osx")) {       // Mac OS Xの場合
        imControl = new IMControl() {
          @Override public void disable(final JComponent component) {
            new Thread(new Runnable() {
              @Override public void run() {
                try {
                  Robot robot = new Robot();
                  // 10回トライして入力モードをUSに切り替える
                  for (int i = 0; i < 10; i++) {
                    // USモードになってればおしまい
                    if (component.getInputContext().getLocale().equals(Locale.US)) break;
                    // そうでなければ opt + cmd + SPACE を送る
                    robot.keyPress(KeyEvent.VK_META);
                    robot.keyPress(KeyEvent.VK_ALT);
                    robot.keyPress(KeyEvent.VK_SPACE);
                    robot.keyRelease(KeyEvent.VK_SPACE);
                    robot.keyRelease(KeyEvent.VK_ALT);
                    robot.keyRelease(KeyEvent.VK_META);
                    Thread.sleep(200);    // wait
                  }
                } catch (Exception ignore) {}
              }
            }, "IMControlThread").start();
          }
        };
      }
    }

    public void execute(Editor editor, DataContext context) {
      imControl.disable(editor.getComponent());
      CommandGroups.getInstance().getChange().processEscape(InjectedLanguageUtil.getTopLevelEditor(editor), context);
    }
  }
}
