/*
 * AceEditorEditLinesHelper.java
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.editors.text;

import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedHandler;

import com.google.gwt.event.shared.HandlerRegistration;

public class AceEditorEditLinesHelper implements CursorChangedHandler
{
   public AceEditorEditLinesHelper(AceEditor editor)
   {
      editor_ = editor;
   }
   
   public void editLinesFromStart()
   {
      int state = state_;
      state_ = (state_ + 1) % 4;
      
      if (cursorChangedHandler_ == null)
         cursorChangedHandler_ = editor_.addCursorChangedHandler(this);
      
      isPerformingEditLinesAction_ = true;
      
      switch (state)
      {
      
      case 0:
         editLinesFromStart(getSelectionExtent(), false, false);
         break;
         
      case 1:
         editLinesFromStart(getSelectionExtent(), false, true);
         break;
         
      case 2:
         editLinesFromStart(getSelectionExtent(), true, false);
         break;
         
      case 3:
         editLinesFromStart(getSelectionExtent(), true, true);
         break;
         
      }
      
      isPerformingEditLinesAction_ = false;
   }
   
   private void editLinesFromStart(Range range,
                                   boolean ignoreBlankLines,
                                   boolean useCommonIndent)
   {
      if (editor_.inMultiSelectMode())
         editor_.exitMultiSelectMode();
      
      int startRow = range.getStart().getRow();
      int endRow = range.getEnd().getRow();
      
      if (ignoreBlankLines)
      {
         for (; startRow <= endRow; startRow++)
         {
            String line = editor_.getLine(startRow);
            if (line.trim().isEmpty())
               continue;

            break;
         }

         // Check to see that we found a non-blank line
         String line = editor_.getLine(startRow);
         if (line.trim().isEmpty())
            return;
      }
      
      String line = editor_.getLine(startRow);
      String indent = StringUtil.getIndent(line);
      int indentSize = indent.length();
      
      if (useCommonIndent)
      {
         for (int row = startRow + 1; row <= endRow; row++)
         {
            line = editor_.getLine(row);
            if (ignoreBlankLines && line.trim().isEmpty())
               continue;

            indent = StringUtil.getIndent(line);
            indentSize = Math.min(indentSize, indent.length());
         }
      }
      
      // Place first cursor at requested location
      editor_.setCursorPosition(Position.create(startRow, indentSize));
      
      // Add new cursors for items in range
      for (int row = startRow + 1; row <= endRow; row++)
      {
         line = editor_.getLine(row);
         if (ignoreBlankLines && line.trim().isEmpty())
            continue;
         
         if (!useCommonIndent)
         {
            indent = StringUtil.getIndent(line);
            indentSize = indent.length();
         }
         
         Range cursorRange = Range.create(row, indentSize, row, indentSize);
         editor_.getNativeSelection().addRange(cursorRange, true);
      }
   }
   
   private Range getSelectionExtent()
   {
      int startRow = editor_.getRowCount();
      int endRow   = 0;
      
      for (Range range : editor_.getNativeSelection().getAllRanges())
      {
         startRow = Math.min(startRow, range.getStart().getRow());
         endRow   = Math.max(endRow, range.getEnd().getRow());
      }
      
      return Range.create(startRow, 0, endRow, 0);
      
   }
   
   @Override
   public void onCursorChanged(CursorChangedEvent event)
   {
      if (!isPerformingEditLinesAction_)
      {
         state_ = 0;
         cursorChangedHandler_.removeHandler();
         cursorChangedHandler_ = null;
      }
   }
   
   private final AceEditor editor_;
   private HandlerRegistration cursorChangedHandler_;
   private int state_ = 0;
   private boolean isPerformingEditLinesAction_;
}
