// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package org.psilynx.psikit.core.wpi.math;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.psilynx.psikit.core.wpi.MathUtil;
import org.psilynx.psikit.core.wpi.StructSerializable;

import java.util.Objects;

/** A rotation in a 3D coordinate frame represented by a quaternion. */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class Rotation3d implements StructSerializable {
  /**
   * A preallocated Rotation3d representing no rotation.
   *
   * <p>This exists to avoid allocations for common rotations.
   */
  public static final Rotation3d kZero = new Rotation3d();

  private final Quaternion m_q;

  /** Constructs a Rotation3d representing no rotation. */
  public Rotation3d() {
    m_q = new Quaternion();
  }

  /**
   * Constructs a Rotation3d from a quaternion.
   *
   * @param q The quaternion.
   */
  @JsonCreator
  public Rotation3d(@JsonProperty(required = true, value = "quaternion") Quaternion q) {
    m_q = q.normalize();
  }

  /**
   * Constructs a Rotation3d from extrinsic roll, pitch, and yaw.
   *
   * <p>Extrinsic rotations occur in that order around the axes in the fixed global frame rather
   * than the body frame.
   *
   * <p>Angles are measured counterclockwise with the rotation axis pointing "out of the page". If
   * you point your right thumb along the positive axis direction, your fingers curl in the
   * direction of positive rotation.
   *
   * @param roll The counterclockwise rotation angle around the X axis (roll) in radians.
   * @param pitch The counterclockwise rotation angle around the Y axis (pitch) in radians.
   * @param yaw The counterclockwise rotation angle around the Z axis (yaw) in radians.
   */
  public Rotation3d(double roll, double pitch, double yaw) {
    // https://en.wikipedia.org/wiki/Conversion_between_quaternions_and_Euler_angles#Euler_angles_to_quaternion_conversion
    double cr = Math.cos(roll * 0.5);
    double sr = Math.sin(roll * 0.5);

    double cp = Math.cos(pitch * 0.5);
    double sp = Math.sin(pitch * 0.5);

    double cy = Math.cos(yaw * 0.5);
    double sy = Math.sin(yaw * 0.5);

    m_q =
        new Quaternion(
            cr * cp * cy + sr * sp * sy,
            sr * cp * cy - cr * sp * sy,
            cr * sp * cy + sr * cp * sy,
            cr * cp * sy - sr * sp * cy);
  }

  /**
   * Constructs a 3D rotation from a 2D rotation in the X-Y plane.
   *
   * @param rotation The 2D rotation.
   * @see Pose3d#Pose3d(Pose2d)
   */
  public Rotation3d(Rotation2d rotation) {
    this(0.0, 0.0, rotation.getRadians());
  }

  /**
   * Adds two rotations together.
   *
   * @param other The rotation to add.
   * @return The sum of the two rotations.
   */
  public Rotation3d plus(Rotation3d other) {
    return rotateBy(other);
  }

  /**
   * Subtracts the new rotation from the current rotation and returns the new rotation.
   *
   * @param other The rotation to subtract.
   * @return The difference between the two rotations.
   */
  public Rotation3d minus(Rotation3d other) {
    return rotateBy(other.unaryMinus());
  }

  /**
   * Takes the inverse of the current rotation.
   *
   * @return The inverse of the current rotation.
   */
  public Rotation3d unaryMinus() {
    return new Rotation3d(m_q.inverse());
  }

  /**
   * Multiplies the current rotation by a scalar.
   *
   * @param scalar The scalar.
   * @return The new scaled Rotation3d.
   */
  public Rotation3d times(double scalar) {
    double angle = 2.0 * Math.acos(m_q.getW());
    double sinHalf = Math.sin(angle / 2.0);

    if (sinHalf == 0.0) {
      return new Rotation3d(); // identity
    }

    double ux = m_q.getX() / sinHalf;
    double uy = m_q.getY() / sinHalf;
    double uz = m_q.getZ() / sinHalf;

    double scaledAngle = scalar * angle;
    double half = scaledAngle / 2.0;

    double w = Math.cos(half);
    double s = Math.sin(half);

    return new Rotation3d(new Quaternion(w, ux * s, uy * s, uz * s));
    // rewritten to not used Vectors, I'm pretty sure this works - Avery
  }

  /**
   * Divides the current rotation by a scalar.
   *
   * @param scalar The scalar.
   * @return The new scaled Rotation3d.
   */
  public Rotation3d div(double scalar) {
    return times(1.0 / scalar);
  }

  /**
   * Adds the new rotation to the current rotation. The other rotation is applied extrinsically,
   * which means that it rotates around the global axes. For example, {@code new
   * Rotation3d(Units.degreesToRadians(90), 0, 0).rotateBy(new Rotation3d(0,
   * Units.degreesToRadians(45), 0))} rotates by 90 degrees around the +X axis and then by 45
   * degrees around the global +Y axis. (This is equivalent to {@code new
   * Rotation3d(Units.degreesToRadians(90), Units.degreesToRadians(45), 0)})
   *
   * @param other The extrinsic rotation to rotate by.
   * @return The new rotated Rotation3d.
   */
  public Rotation3d rotateBy(Rotation3d other) {
    return new Rotation3d(other.m_q.times(m_q));
  }

  /**
   * Returns the quaternion representation of the Rotation3d.
   *
   * @return The quaternion representation of the Rotation3d.
   */
  @JsonProperty(value = "quaternion")
  public Quaternion getQuaternion() {
    return m_q;
  }

  /**
   * Returns the counterclockwise rotation angle around the X axis (roll) in radians.
   *
   * @return The counterclockwise rotation angle around the X axis (roll) in radians.
   */
  public double getX() {
    final var w = m_q.getW();
    final var x = m_q.getX();
    final var y = m_q.getY();
    final var z = m_q.getZ();

    // wpimath/algorithms.md
    final var cxcy = 1.0 - 2.0 * (x * x + y * y);
    final var sxcy = 2.0 * (w * x + y * z);
    final var cy_sq = cxcy * cxcy + sxcy * sxcy;
    if (cy_sq > 1e-20) {
      return Math.atan2(sxcy, cxcy);
    } else {
      return 0.0;
    }
  }

  /**
   * Returns the counterclockwise rotation angle around the Y axis (pitch) in radians.
   *
   * @return The counterclockwise rotation angle around the Y axis (pitch) in radians.
   */
  public double getY() {
    final var w = m_q.getW();
    final var x = m_q.getX();
    final var y = m_q.getY();
    final var z = m_q.getZ();

    // https://en.wikipedia.org/wiki/Conversion_between_quaternions_and_Euler_angles#Quaternion_to_Euler_angles_(in_3-2-1_sequence)_conversion
    double ratio = 2.0 * (w * y - z * x);
    if (Math.abs(ratio) >= 1.0) {
      return Math.copySign(Math.PI / 2.0, ratio);
    } else {
      return Math.asin(ratio);
    }
  }

  /**
   * Returns the counterclockwise rotation angle around the Z axis (yaw) in radians.
   *
   * @return The counterclockwise rotation angle around the Z axis (yaw) in radians.
   */
  public double getZ() {
    final var w = m_q.getW();
    final var x = m_q.getX();
    final var y = m_q.getY();
    final var z = m_q.getZ();

    // wpimath/algorithms.md
    final var cycz = 1.0 - 2.0 * (y * y + z * z);
    final var cysz = 2.0 * (w * z + x * y);
    final var cy_sq = cycz * cycz + cysz * cysz;
    if (cy_sq > 1e-20) {
      return Math.atan2(cysz, cycz);
    } else {
      return Math.atan2(2.0 * w * z, w * w - z * z);
    }
  }

  /**
   * Returns the angle in radians in the axis-angle representation of this rotation.
   *
   * @return The angle in radians in the axis-angle representation of this rotation.
   */
  public double getAngle() {
    double norm =
        Math.sqrt(m_q.getX() * m_q.getX() + m_q.getY() * m_q.getY() + m_q.getZ() * m_q.getZ());
    return 2.0 * Math.atan2(norm, m_q.getW());
  }

  /**
   * Returns a Rotation2d representing this Rotation3d projected into the X-Y plane.
   *
   * @return A Rotation2d representing this Rotation3d projected into the X-Y plane.
   */
  public Rotation2d toRotation2d() {
    return new Rotation2d(getZ());
  }

  @Override
  public String toString() {
    return String.format("Rotation3d(%s)", m_q);
  }

  /**
   * Checks equality between this Rotation3d and another object.
   *
   * @param obj The other object.
   * @return Whether the two objects are equal or not.
   */
  @Override
  public boolean equals(Object obj) {
    return obj instanceof Rotation3d
        && Math.abs(Math.abs(m_q.dot(((Rotation3d)obj).m_q)) - m_q.norm() * ((Rotation3d)obj).m_q.norm()) < 1e-9;
  }

  @Override
  public int hashCode() {
    return Objects.hash(m_q);
  }

  public Rotation3d interpolate(Rotation3d endValue, double t) {
    return plus(endValue.minus(this).times(MathUtil.clamp(t, 0, 1)));
  }


  /** Rotation3d struct for serialization. */
  public static final Rotation3dStruct struct = new Rotation3dStruct();
}
