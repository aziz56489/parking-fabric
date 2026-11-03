-- Converted from the supplied H2 2.2.224 export.
-- Select the target database before importing this file.

CREATE TABLE IF NOT EXISTS parking_area (
  id INT NOT NULL AUTO_INCREMENT,
  capacity INT NOT NULL,
  name VARCHAR(255) UNIQUE,
  layout LONGTEXT,
  PRIMARY KEY (id)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS parking_spot (
  id INT NOT NULL AUTO_INCREMENT,
  parking_area_id INT,
  number VARCHAR(255),
  status ENUM('AVAILABLE', 'NOT_AVAILABLE', 'OCCUPIED', 'RESERVED'),
  type ENUM('EV', 'HANDICAPPED', 'PERSONNEL', 'REGULAR'),
  PRIMARY KEY (id),
  CONSTRAINT fk_parking_spot_area FOREIGN KEY (parking_area_id) REFERENCES parking_area(id)
) ENGINE=InnoDB AUTO_INCREMENT=45 DEFAULT CHARSET=utf8mb4;

INSERT INTO parking_area (capacity, id, name, layout) VALUES
(44, 1, 'hamza', '{"width":42,"height":25,"latitude":null,"longitude":null,"floors":[[{"kind":"GREEN_SPACE","x":0,"y":0,"width":40,"height":0.5,"angle":0},{"kind":"GREEN_SPACE","x":0,"y":0.5,"width":0.5,"height":24.5,"angle":0},{"kind":"ROAD","x":0.5,"y":5.5,"width":39.5,"height":5,"angle":0},{"kind":"ROAD","x":0.5,"y":10.5,"width":3.5,"height":14.5,"angle":0},{"kind":"ROAD","x":4,"y":20,"width":36,"height":5,"angle":0},{"kind":"ENTRANCE","x":40,"y":5.5,"width":2,"height":2.5,"angle":0},{"kind":"EXIT","x":40,"y":8,"width":2,"height":2.5,"angle":0},{"kind":"ENTRANCE","x":40,"y":20,"width":2,"height":2.5,"angle":0},{"kind":"EXIT","x":40,"y":22.5,"width":2,"height":2.5,"angle":0},{"kind":"SPOT","type":"REGULAR","number":"A-01","x":0.5,"y":0.5,"width":2.5,"height":5,"angle":0},{"kind":"SPOT","type":"REGULAR","number":"A-02","x":3,"y":0.5,"width":2.5,"height":5,"angle":0},{"kind":"SPOT","type":"REGULAR","number":"A-03","x":10.5,"y":0.5,"width":2.5,"height":5,"angle":0},{"kind":"SPOT","type":"REGULAR","number":"A-04","x":5.5,"y":0.5,"width":2.5,"height":5,"angle":0},{"kind":"SPOT","type":"REGULAR","number":"A-05","x":8,"y":0.5,"width":2.5,"height":5,"angle":0},{"kind":"SPOT","type":"REGULAR","number":"A-06","x":13,"y":0.5,"width":2.5,"height":5,"angle":0},{"kind":"SPOT","type":"REGULAR","number":"A-07","x":15.5,"y":0.5,"width":2.5,"height":5,"angle":0},{"kind":"SPOT","type":"REGULAR","number":"A-08","x":18,"y":0.5,"width":2.5,"height":5,"angle":0},{"kind":"SPOT","type":"REGULAR","number":"A-09","x":20.5,"y":0.5,"width":2.5,"height":5,"angle":0},{"kind":"SPOT","type":"REGULAR","number":"A-10","x":23,"y":0.5,"width":2,"height":5,"angle":0},{"kind":"SPOT","type":"REGULAR","number":"A-11","x":25,"y":0.5,"width":2.5,"height":5,"angle":0},{"kind":"SPOT","type":"REGULAR","number":"A-12","x":27.5,"y":0.5,"width":2.5,"height":5,"angle":0},{"kind":"SPOT","type":"REGULAR","number":"A-13","x":30,"y":0.5,"width":2.5,"height":5,"angle":0},{"kind":"SPOT","type":"REGULAR","number":"A-14","x":32.5,"y":0.5,"width":2.5,"height":5,"angle":0},{"kind":"SPOT","type":"REGULAR","number":"A-15","x":35,"y":0.5,"width":2.5,"height":5,"angle":0},{"kind":"SPOT","type":"REGULAR","number":"A-16","x":37.5,"y":0.5,"width":2.5,"height":5,"angle":0},{"kind":"SPOT","type":"REGULAR","number":"A-17","x":4,"y":10.5,"width":2.5,"height":4.5,"angle":0},{"kind":"SPOT","type":"REGULAR","number":"A-18","x":4,"y":15,"width":2.5,"height":5,"angle":0},{"kind":"SPOT","type":"REGULAR","number":"A-19","x":6.5,"y":10.5,"width":2.5,"height":4.5,"angle":0},{"kind":"SPOT","type":"REGULAR","number":"A-20","x":9,"y":10.5,"width":2.5,"height":4.5,"angle":0},{"kind":"SPOT","type":"REGULAR","number":"A-21","x":12,"y":10.5,"width":2.5,"height":4.5,"angle":0},{"kind":"SPOT","type":"REGULAR","number":"A-22","x":14.5,"y":10.5,"width":2.5,"height":4.5,"angle":0},{"kind":"SPOT","type":"REGULAR","number":"A-23","x":17,"y":10.5,"width":2.5,"height":4.5,"angle":0},{"kind":"SPOT","type":"REGULAR","number":"A-24","x":6.5,"y":15,"width":2.5,"height":5,"angle":0},{"kind":"SPOT","type":"REGULAR","number":"A-25","x":9,"y":15,"width":2.5,"height":5,"angle":0},{"kind":"SPOT","type":"REGULAR","number":"A-26","x":12,"y":15,"width":2.5,"height":5,"angle":0},{"kind":"SPOT","type":"REGULAR","number":"A-27","x":14.5,"y":15,"width":2.5,"height":5,"angle":0},{"kind":"SPOT","type":"REGULAR","number":"A-28","x":17,"y":15,"width":2.5,"height":5,"angle":0},{"kind":"SPOT","type":"REGULAR","number":"A-29","x":19.5,"y":15,"width":2.5,"height":5,"angle":0},{"kind":"SPOT","type":"REGULAR","number":"A-30","x":22,"y":15,"width":2.5,"height":5,"angle":0},{"kind":"SPOT","type":"REGULAR","number":"A-31","x":24.5,"y":15,"width":2.5,"height":5,"angle":0},{"kind":"SPOT","type":"REGULAR","number":"A-32","x":27.5,"y":15,"width":2.5,"height":5,"angle":0},{"kind":"SPOT","type":"REGULAR","number":"A-33","x":30,"y":15,"width":2.5,"height":5,"angle":0},{"kind":"SPOT","type":"REGULAR","number":"A-34","x":32.5,"y":15,"width":2.5,"height":5,"angle":0},{"kind":"SPOT","type":"REGULAR","number":"A-35","x":35,"y":15,"width":2.5,"height":5,"angle":0},{"kind":"SPOT","type":"REGULAR","number":"A-36","x":37.5,"y":15,"width":2.5,"height":5,"angle":0},{"kind":"SPOT","type":"REGULAR","number":"A-37","x":19.5,"y":10.5,"width":2.5,"height":4.5,"angle":0},{"kind":"SPOT","type":"REGULAR","number":"A-38","x":22,"y":10.5,"width":2.5,"height":4.5,"angle":0},{"kind":"SPOT","type":"REGULAR","number":"A-39","x":24.5,"y":10.5,"width":2.5,"height":4.5,"angle":0},{"kind":"SPOT","type":"REGULAR","number":"A-40","x":27.5,"y":10.5,"width":2.5,"height":4.5,"angle":0},{"kind":"SPOT","type":"REGULAR","number":"A-41","x":30,"y":10.5,"width":2.5,"height":4.5,"angle":0},{"kind":"SPOT","type":"REGULAR","number":"A-42","x":32.5,"y":10.5,"width":2.5,"height":4.5,"angle":0},{"kind":"SPOT","type":"REGULAR","number":"A-43","x":35,"y":10.5,"width":2.5,"height":4.5,"angle":0},{"kind":"SPOT","type":"REGULAR","number":"A-44","x":37.5,"y":10.5,"width":2.5,"height":4.5,"angle":0},{"kind":"WALL","x":42,"y":10.5,"width":10,"height":2,"angle":90},{"kind":"WALL","x":42,"y":0,"width":5.5,"height":2,"angle":90}]]}');

INSERT INTO parking_spot (id, parking_area_id, number, status, type) VALUES
(1, 1, 'A-08', 'AVAILABLE', 'REGULAR'),
(2, 1, 'A-09', 'AVAILABLE', 'REGULAR'),
(3, 1, 'A-06', 'AVAILABLE', 'REGULAR'),
(4, 1, 'A-28', 'AVAILABLE', 'REGULAR'),
(5, 1, 'A-07', 'AVAILABLE', 'REGULAR'),
(6, 1, 'A-29', 'AVAILABLE', 'REGULAR'),
(7, 1, 'A-04', 'AVAILABLE', 'REGULAR'),
(8, 1, 'A-26', 'AVAILABLE', 'REGULAR'),
(9, 1, 'A-05', 'AVAILABLE', 'REGULAR'),
(10, 1, 'A-27', 'AVAILABLE', 'REGULAR'),
(11, 1, 'A-02', 'AVAILABLE', 'REGULAR'),
(12, 1, 'A-24', 'AVAILABLE', 'REGULAR'),
(13, 1, 'A-03', 'AVAILABLE', 'REGULAR'),
(14, 1, 'A-25', 'AVAILABLE', 'REGULAR'),
(15, 1, 'A-22', 'AVAILABLE', 'REGULAR'),
(16, 1, 'A-44', 'AVAILABLE', 'REGULAR'),
(17, 1, 'A-01', 'AVAILABLE', 'REGULAR'),
(18, 1, 'A-23', 'AVAILABLE', 'REGULAR'),
(19, 1, 'A-20', 'AVAILABLE', 'REGULAR'),
(20, 1, 'A-42', 'AVAILABLE', 'REGULAR'),
(21, 1, 'A-21', 'AVAILABLE', 'REGULAR'),
(22, 1, 'A-43', 'AVAILABLE', 'REGULAR'),
(23, 1, 'A-40', 'AVAILABLE', 'REGULAR'),
(24, 1, 'A-41', 'AVAILABLE', 'REGULAR'),
(25, 1, 'A-19', 'AVAILABLE', 'REGULAR'),
(26, 1, 'A-17', 'AVAILABLE', 'REGULAR'),
(27, 1, 'A-39', 'AVAILABLE', 'REGULAR'),
(28, 1, 'A-18', 'AVAILABLE', 'REGULAR'),
(29, 1, 'A-15', 'AVAILABLE', 'REGULAR'),
(30, 1, 'A-37', 'AVAILABLE', 'REGULAR'),
(31, 1, 'A-16', 'AVAILABLE', 'REGULAR'),
(32, 1, 'A-38', 'AVAILABLE', 'REGULAR'),
(33, 1, 'A-13', 'AVAILABLE', 'REGULAR'),
(34, 1, 'A-35', 'AVAILABLE', 'REGULAR'),
(35, 1, 'A-14', 'AVAILABLE', 'REGULAR'),
(36, 1, 'A-36', 'AVAILABLE', 'REGULAR'),
(37, 1, 'A-11', 'AVAILABLE', 'REGULAR'),
(38, 1, 'A-33', 'AVAILABLE', 'REGULAR'),
(39, 1, 'A-12', 'AVAILABLE', 'REGULAR'),
(40, 1, 'A-34', 'AVAILABLE', 'REGULAR'),
(41, 1, 'A-31', 'AVAILABLE', 'REGULAR'),
(42, 1, 'A-10', 'AVAILABLE', 'REGULAR'),
(43, 1, 'A-32', 'AVAILABLE', 'REGULAR'),
(44, 1, 'A-30', 'AVAILABLE', 'REGULAR');