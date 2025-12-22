// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package org.psilynx.psikit.core.wpi.math;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.psilynx.psikit.core.wpi.StructSerializable;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Represents a 2D pose containing translational and rotational elements. */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class Pose2d implements StructSerializable {
  /**
   * A preallocated Pose2d representing the origin.
   *
   * <p>This exists to avoid allocations for common poses.
   */
  public static final Pose2d kZero = new Pose2d();

  private final Translation2d m_translation;
  private final Rotation2d m_rotation;

  /** Constructs a pose at the origin facing toward the positive X axis. */
  public Pose2d() {
    m_translation = Translation2d.kZero;
    m_rotation = Rotation2d.kZero;
  }

  /**
   * Constructs a pose with the specified translation and rotation.
   *
   * @param translation The translational component of the pose.
   * @param rotation The rotational component of the pose.
   */
  @JsonCreator
  public Pose2d(
      @JsonProperty(required = true, value = "translation") Translation2d translation,
      @JsonProperty(required = true, value = "rotation") Rotation2d rotation) {
    m_translation = translation;
    m_rotation = rotation;
  }

  /**
   * Constructs a pose with x and y translations instead of a separate Translation2d.
   *
   * @param x The x component of the translational component of the pose.
   * @param y The y component of the translational component of the pose.
   * @param rotation The rotational component of the pose.
   */
  public Pose2d(double x, double y, Rotation2d rotation) {
    m_translation = new Translation2d(x, y);
    m_rotation = rotation;
  }

  /**
   * Returns the translation component of the transformation.
   *
   * @return The translational component of the pose.
   */
  @JsonProperty
  public Translation2d getTranslation() {
    return m_translation;
  }

  /**
   * Returns the X component of the pose's translation.
   *
   * @return The x component of the pose's translation.
   */
  public double getX() {
    return m_translation.getX();
  }

  /**
   * Returns the Y component of the pose's translation.
   *
   * @return The y component of the pose's translation.
   */
  public double getY() {
    return m_translation.getY();
  }

  /**
   * Returns the rotational component of the transformation.
   *
   * @return The rotational component of the pose.
   */
  @JsonProperty
  public Rotation2d getRotation() {
    return m_rotation;
  }

  /**
   * Multiplies the current pose by a scalar.
   *
   * @param scalar The scalar.
   * @return The new scaled Pose2d.
   */
  public Pose2d times(double scalar) {
    return new Pose2d(m_translation.times(scalar), m_rotation.times(scalar));
  }

  /**
   * Divides the current pose by a scalar.
   *
   * @param scalar The scalar.
   * @return The new scaled Pose2d.
   */
  public Pose2d div(double scalar) {
    return times(1.0 / scalar);
  }

  /**
   * Rotates the pose around the origin and returns the new pose.
   *
   * @param other The rotation to transform the pose by.
   * @return The transformed pose.
   */
  public Pose2d rotateBy(Rotation2d other) {
    return new Pose2d(m_translation.rotateBy(other), m_rotation.rotateBy(other));
  }

  /**
   * Rotates the current pose around a point in 2D space.
   *
   * @param point The point in 2D space to rotate around.
   * @param rot The rotation to rotate the pose by.
   * @return The new rotated pose.
   */
  public Pose2d rotateAround(Translation2d point, Rotation2d rot) {
    return new Pose2d(m_translation.rotateAround(point, rot), m_rotation.rotateBy(rot));
  }

  /**
   * Returns the nearest Pose2d from a list of poses. If two or more poses in the list have the same
   * distance from this pose, return the one with the closest rotation component.
   *
   * @param poses The list of poses to find the nearest.
   * @return The nearest Pose2d from the list.
   */
  public Pose2d nearest(List<Pose2d> poses) {
    return Collections.min(
        poses,
        Comparator.comparing(
                (Pose2d other) -> this.getTranslation().getDistance(other.getTranslation()))
            .thenComparing(
                (Pose2d other) ->
                    Math.abs(this.getRotation().minus(other.getRotation()).getRadians())));
  }

  @Override
  public String toString() {
    return String.format("Pose2d(%s, %s)", m_translation, m_rotation);
  }

  /**
   * Checks equality between this Pose2d and another object.
   *
   * @param obj The other object.
   * @return Whether the two objects are equal or not.
   */
  @Override
  public boolean equals(Object obj) {
    return obj instanceof Pose2d
        && m_translation.equals(((Pose2d) obj).m_translation)
        && m_rotation.equals(((Pose2d) obj).m_rotation);
  }

  @Override
  public int hashCode() {
    return Objects.hash(m_translation, m_rotation);
  }

  /** Pose2d struct for serialization. */
  public static final Pose2dStruct struct = new Pose2dStruct();
}
