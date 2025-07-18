// Copyright (c) 2021-2025 Littleton Robotics
// http://github.com/Mechanical-Advantage
//
// Use of this source code is governed by a BSD
// license that can be found in the LICENSE file
// at the root directory of this project.

package org.psilynx.psikit;

public interface ConsoleSource extends AutoCloseable {
  /** Reads all console data that has been produced since the last call to this method. */
  public String getNewData();
}
