
public class PMO_SimpleGenerator implements PointGeneratorInterface {

	private final Point2D point;
	
	public PMO_SimpleGenerator( Point2D pattern ) {
		point = new Point2D( pattern.firstCoordinate, pattern.secondCoordinate );
	}
		
	@Override
	public Point2D getPoint() {
		return point;
	}

}
